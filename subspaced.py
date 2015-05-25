import pickle

from ConfigParser import SafeConfigParser
from os.path import expanduser
from OpenSSL import SSL
from txjsonrpc.netstring import jsonrpc

from twisted.application import service, internet
from twisted.python.log import ILogObserver
from twisted.internet import ssl, task
from twisted.web import resource, server
from twisted.web.resource import NoResource

from subspace.network import Server
from subspace import log
from subspace.message import *



sys.path.append(os.path.dirname(__file__))

datafolder = expanduser("~") + "/.subspace/"

f = file(datafolder + 'subspace.conf')
cfg = SafeConfigParser()
cfg.read(f)

username = cfg.get("SUBSPACED", "rpcusername") if cfg.has_option("SUBSPACED", "rpcusername") else "Username"
password = cfg.get("SUBSPACED", "rpcpassword") if cfg.has_option("SUBSPACED", "rpcpassword") else "Password"
bootstrap_node = cfg.get("SUBSPACED", "bootstrapnode") if cfg.has_option("SUBSPACED", "bootstrapnode") else "1.2.3.4"
bootstrap_port = cfg.get("SUBSPACED", "bootstrapport") if cfg.has_option("SUBSPACED", "bootstrapport") else "8335"

if os.path.isfile(datafolder + 'keys.pickle'):
    privkey = pickle.load(open(datafolder + "keys.pickle", "rb"))
else:
    privkey = random_key()
    pickle.dump(privkey, open(datafolder + "keys.pickle", "wb"))

pubkey = encode_pubkey(privkey_to_pubkey(privkey), "hex_compressed")

application = service.Application("subspace")
application.setComponent(ILogObserver, log.FileLogObserver(sys.stdout, log.INFO).emit)

if os.path.isfile('cache.pickle'):
    kserver = Server.loadState('cache.pickle')
else:
    kserver = Server(id=pubkey[2:42])
    kserver.bootstrap([(bootstrap_node, bootstrap_port)])
kserver.saveStateRegularly('cache.pickle', 10)
udpserver = internet.UDPServer(cfg.get("SUBSPACED", "port") if cfg.has_option("SUBSPACED", "port") else 8335, kserver.protocol)
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

if cfg.has_option("SUBSPACED", "server"):
    server_protocol = server.Site(WebResource(kserver))
    if cfg.has_option("SUBSPACED", "useSSL"):
        webserver = internet.SSLServer(cfg.get("SUBSPACED", "serverport") if cfg.has_option("SUBSPACED", "serverport") else 8080,
                                   server_protocol,
                                   ChainedOpenSSLContextFactory(cfg.get("SUBSPACED", "sslkey"), cfg.get("SUBSPACED", "sslcert")))
        #webserver = internet.SSLServer(8335, website, ssl.DefaultOpenSSLContextFactory(options["sslkey"], options["sslcert"]))
    else:
        webserver = internet.TCPServer(cfg.get("SUBSPACED", "serverport") if cfg.has_option("SUBSPACED", "serverport") else 8080, server_protocol)
    webserver.setServiceParent(application)

# RPC-Server
class RPCCalls(jsonrpc.JSONRPC):
    """An example object to be published."""

    def jsonrpc_getpubkey(self):
        return pubkey

    def jsonrpc_getprivkey(self):
        return privkey

    def jsonrpc_getmessages(self):
        return MessageDecoder(privkey, kserver).getMessages()

    def jsonrpc_send(self, pubkey, message):
        r = kserver.getRange()
        if r is False:
            return "Counldn't find any peers. Maybe check your internet connection?"
        else:
            message = MessageEncoder(pubkey, privkey, message, r).get_messages()
            for key, value in message.items():
                log.msg("Setting %s = %s" % (key, value))
                kserver.set(key, value)
            return "Message sent successfully"

factory = jsonrpc.RPCFactory(RPCCalls)

factory.addIntrospection()

jsonrpcServer = internet.TCPServer(7080, factory, interface='127.0.0.1')
jsonrpcServer.setServiceParent(application)

