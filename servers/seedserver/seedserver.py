__author__ = 'chris'
import sys, os
import gzip

from cStringIO import StringIO

from OpenSSL import SSL

from twisted.application import service, internet
from twisted.python.log import ILogObserver
from twisted.internet import ssl, task, reactor
from twisted.web import resource, server
from twisted.web.resource import NoResource

from subspace.network import Server
from subspace import log
from subspace.node import Node
from subspace.crawling import NodeSpiderCrawl

from servers.seedserver import peerseeds

from random import shuffle

from bitcoin import *

sys.path.append(os.path.dirname(__file__))

ssl_key = "/path/to/ssl.key"
ssl_cert = "/path/to/ssl.cert"

bootstrap_list = [("1.2.4.5", 8335)]

application = service.Application("subspace_seed_server")
application.setComponent(ILogObserver, log.FileLogObserver(sys.stdout, log.INFO).emit)

if os.path.isfile('cache.pickle'):
    kserver = Server.loadState('cache.pickle')
else:
    kserver = Server(id=random_key()[:40])
    kserver.bootstrap(bootstrap_list)

kserver.saveStateRegularly('cache.pickle', 10)
udpserver = internet.UDPServer(8336, kserver.protocol)
udpserver.setServiceParent(application)

class ChainedOpenSSLContextFactory(ssl.DefaultOpenSSLContextFactory):
    def __init__(self, privateKeyFileName, certificateChainFileName,
                 sslmethod=SSL.SSLv23_METHOD):
        """
        @param privateKeyFileName: Name of a file containing a private key
        @param certificateChainFileName: Name of a file containing a certificate chain
        @param sslmethod: The SSL method to use
        """
        self.privateKeyFileName = privateKeyFileName
        self.certificateChainFileName = certificateChainFileName
        self.sslmethod = sslmethod
        self.cacheContext()

    def cacheContext(self):
        ctx = SSL.Context(self.sslmethod)
        ctx.use_certificate_chain_file(self.certificateChainFileName)
        ctx.use_privatekey_file(self.privateKeyFileName)
        self._context = ctx

class WebResource(resource.Resource):
    def __init__(self, kserver):
        resource.Resource.__init__(self)
        self.kserver = kserver
        loopingCall = task.LoopingCall(self.crawl)
        loopingCall.start(60, True)

    def crawl(self):
        for bucket in self.kserver.protocol.router.buckets:
            for node in bucket.getNodes():
                self.kserver.protocol.callPing(node)
        node = Node(random_key[:40])
        nearest = self.kserver.protocol.router.findNeighbors(node)
        spider = NodeSpiderCrawl(self.kserver.protocol, node, nearest, 100, self.alpha)
        spider.find()

    def getChild(self, child, request):
        return self

    def render_GET(self, request):
        def respond(uncompressed_data):
            buf = StringIO()
            f = gzip.GzipFile(mode='wb', fileobj=buf)
            try:
                f.write(uncompressed_data)
            finally:
                f.close()
            compressed_data = buf.getvalue()
            request.write(compressed_data)
            request.finish()

        nodes = []
        for bucket in self.kserver.protocol.router.buckets:
            nodes.append(bucket.getNodes())
        shuffle(nodes)
        seeds = peerseeds.PeerSeeds()
        for node in nodes[:50]:
            data = peerseeds.PeerSeedData()
            data.ip_address = node.ip
            data.port = node.port
            #TODO add in services after the wire protocol is updated
            seeds.PeerSeedData.append(data)
        seeds.timestamp = int(time.time())
        seeds.net = "main"
        log.msg("Received a request for nodes, responding...")
        respond(seeds.SerializeToString())

server_protocol = server.Site(WebResource(kserver))
seed_server = internet.SSLServer(8080, server_protocol, ChainedOpenSSLContextFactory(ssl_key, ssl_cert))
seed_server.setServiceParent(application)
