__author__ = 'chris'
import sys, os
import gzip

from cStringIO import StringIO

from OpenSSL import SSL

from twisted.application import service, internet
from twisted.python.log import ILogObserver
from twisted.internet import ssl, task, reactor
from twisted.web import resource, server

from subspace.network import Server
from subspace import log
from subspace.node import Node
from subspace.crawling import NodeSpiderCrawl

from servers.seedserver import peerseeds

from binascii import unhexlify

from random import shuffle

from bitcoin import *

sys.path.append(os.path.dirname(__file__))

ssl_key = "/path/to/ssl.key"
ssl_cert = "/path/to/ssl.cert"

bootstrap_list = [("1.2.4.5", 8335)]

application = service.Application("subspace_seed_server")
application.setComponent(ILogObserver, log.FileLogObserver(sys.stdout, log.INFO).emit)

node_id = unhexlify(random_key())
this_node = Node(node_id, "the_server_ip_address", 8335)

if os.path.isfile('cache.pickle'):
    kserver = Server.loadState('cache.pickle')
else:
    kserver = Server(id=node_id)
    kserver.bootstrap(bootstrap_list)

kserver.saveStateRegularly('cache.pickle', 10)
udpserver = internet.UDPServer(8335, kserver.protocol)
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
        self.protobuf = []
        self.json = []
        loopingCall = task.LoopingCall(self.crawl)
        loopingCall.start(60, True)

    def crawl(self):
        def gather_results(result):
            nodes = []
            for bucket in self.kserver.protocol.router.buckets:
                nodes.extend(bucket.getNodes())
            nodes.append(this_node)
            shuffle(nodes)
            seeds = peerseeds.PeerSeeds()
            json_list = []
            for node in nodes[:50]:
                node_dic = {}
                node_dic["ip"] = node.ip
                node_dic["port"] = node.port
                json_list.append(node_dic)
                data = seeds.seed.add()
                data.ip_address = node.ip
                data.port = node.port
                #TODO add in services after the wire protocol is updated
            seeds.timestamp = int(time.time())
            seeds.net = "main"
            uncompressed_data = seeds.SerializeToString()
            buf = StringIO()
            f = gzip.GzipFile(mode='wb', fileobj=buf)
            try:
                f.write(uncompressed_data)
            finally:
                f.close()
            self.protobuf = buf.getvalue()
            self.json = json.dumps(json_list, indent=4)

        for bucket in self.kserver.protocol.router.buckets:
            for node in bucket.getNodes():
                self.kserver.protocol.callPing(node)
        node = Node(unhexlify(random_key()))
        nearest = self.kserver.protocol.router.findNeighbors(node)
        spider = NodeSpiderCrawl(self.kserver.protocol, node, nearest, 100, 4)
        d = spider.find().addCallback(gather_results)

    def getChild(self, child, request):
        return self

    def render_GET(self, request):
        log.msg("Received a request for nodes, responding...")
        if "format" in request.args:
            if request.args["format"][0] == "json":
                request.write(self.json)
            elif request.args["format"][0] == "protobuf":
                request.write(self.protobuf)
        else:
            request.write(self.protobuf)
        request.finish()
        return server.NOT_DONE_YET

server_protocol = server.Site(WebResource(kserver))
seed_server = internet.SSLServer(8080, server_protocol, ChainedOpenSSLContextFactory(ssl_key, ssl_cert))
seed_server.setServiceParent(application)
