from config import Config
from os.path import expanduser
from twisted.application import service, internet
from twisted.python.log import ILogObserver
from twisted.internet import ssl, task
from twisted.web import resource, server
from twisted.web.resource import NoResource
from OpenSSL import SSL
from subspace.network import Server
from subspace import log
from bitcoin import main


import thread
import sys, os
import pyjsonrpc
import base64
import httplib
import pickle
import json

sys.path.append(os.path.dirname(__file__))

datafolder = expanduser("~") + "/.subspace/"

f = file(datafolder + 'subspace.conf')
cfg = Config(f)

username = cfg.rpcusername if "rpcusername" in cfg else "Username"
password = cfg.rpcpassword if "rpcpassword" in cfg else "Password"
bootstrap_node = cfg.bootstrapnode if "bootstrapnode" in cfg else "1.2.3.4"
bootstrap_port = cfg.bootstrapport if "bootstrapport" in cfg else "8335"

if os.path.isfile(datafolder + 'keys.pickle'):
    privkey = pickle.load(open(datafolder + "keys.pickle", "rb"))
else:
    privkey = main.random_key()
    pickle.dump(privkey, open(datafolder + "keys.pickle", "wb"))

pubkey = main.encode_pubkey(main.privkey_to_pubkey(privkey), "hex_compressed")

application = service.Application("kademlia")
application.setComponent(ILogObserver, log.FileLogObserver(sys.stdout, log.INFO).emit)

if os.path.isfile('cache.pickle'):
    kserver = Server.loadState('cache.pickle')
else:
    kserver = Server()
    kserver.bootstrap([(bootstrap_node, bootstrap_port)])
kserver.saveStateRegularly('cache.pickle', 10)

udpserver = internet.UDPServer(cfg.port if "port" in cfg else 8335, kserver.protocol)
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

# Web-Server
class WebResource(resource.Resource):
    def __init__(self, kserver):
        resource.Resource.__init__(self)
        self.kserver = kserver
        # throttle in seconds to check app for new data
        self.throttle = .25
        # define a list to store client requests
        self.delayed_requests = []
        # define a list to store incoming keys from new POSTs
        self.incoming_posts = []
        # setup a loop to process delayed requests
        loopingCall = task.LoopingCall(self.processDelayedRequests)
        loopingCall.start(self.throttle, False)

    def getChild(self, child, request):
        return self

    def render_GET(self, request):
        def respond(value):
            value = value or NoResource().render(request)
            request.write(value)
            request.finish()
        log.msg("Getting key: %s" % request.path.split('/')[-1])
        d = self.kserver.get(request.path.split('/')[-1])
        if d is not None:
            respond(d)
            return server.NOT_DONE_YET
        else:
            self.delayed_requests.append(request)
            return server.NOT_DONE_YET

    def render_POST(self, request):
        key = request.path.split('/')[-1]
        value = request.content.getvalue()
        log.msg("Setting %s = %s" % (key, value))
        self.kserver.set(key, value)
        self.incoming_posts.append(key)
        return value

    def processDelayedRequests(self):
        """
        Processes the delayed requests that did not have
        any data to return last time around.
        """

if "server" in cfg:
    server_protocol = server.Site(WebResource(kserver))
    if "useSSL" in cfg:
        webserver = internet.SSLServer(cfg.serverport if "serverport" in cfg else 8080,
                                   server_protocol,
                                   ChainedOpenSSLContextFactory(cfg.sslkey, cfg.sslcert))
        #webserver = internet.SSLServer(8335, website, ssl.DefaultOpenSSLContextFactory(options["sslkey"], options["sslcert"]))
    else:
        webserver = internet.TCPServer(cfg.serverport if "serverport" in cfg else 8080, server_protocol)
    webserver.setServiceParent(application)

# Threading RPC-Server
class RequestHandler(pyjsonrpc.HttpRequestHandler):

    @pyjsonrpc.rpcmethod
    def add(self, a, b):
         """Test method"""
         return a + b

    @pyjsonrpc.rpcmethod
    def getpubkey(self):
        return pubkey

    @pyjsonrpc.rpcmethod
    def send(self, key, value):
        log.msg("Setting %s = %s" % (key, value))
        kserver.set(key, value)
        return value

    @pyjsonrpc.rpcmethod
    def getmessages(self):
        data = kserver.storage.get_all()
        return data


class AuthHandler(RequestHandler):

    def do_POST(self):
        address = self.client_address[0]
        key = base64.b64encode(username + ":" + password)
        if self.headers.getheader('Authorization') == None:
            self.send_response(httplib.UNAUTHORIZED)
            pass
        elif self.headers.getheader('Authorization') == 'Basic '+key:
            if address == "127.0.0.1" or (cfg.rpcallowip if "rpcallowip" in cfg else "127.0.0.1"):
                RequestHandler.do_POST(self)
            else:
                self.send_response(httplib.UNAUTHORIZED)
            pass
        else:
            self.send_response(httplib.UNAUTHORIZED)
            pass

rpc_server = pyjsonrpc.ThreadingHttpServer(
    server_address = ('localhost', cfg.rpcport if "rpcport" in cfg else 8336),
    RequestHandlerClass = AuthHandler
)
def start_rpc_server(arg):
    rpc_server.serve_forever()
thread = thread.start_new_thread(start_rpc_server, ("Thread-rpc", ))
