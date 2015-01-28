from twisted.application import service, internet
from twisted.python.log import ILogObserver
from twisted.python import log
from twisted.internet import reactor, task
from twisted.web import resource, server
from twisted.web.resource import NoResource

from pymongo import MongoClient

import sys, os
sys.path.append(os.path.dirname(__file__))
from kademlia.network import Server
from kademlia import log
import datetime
from bson.objectid import ObjectId

#start mongodb -> sudo service mongod start

application = service.Application("kademlia")
application.setComponent(ILogObserver, log.FileLogObserver(sys.stdout, log.INFO).emit)

if os.path.isfile('cache.pickle'):
    kserver = Server.loadState('cache.pickle')
else:
    kserver = Server()
    kserver.bootstrap([("1.2.3.4", 8468)])
kserver.saveStateRegularly('cache.pickle', 10)

udpserver = internet.UDPServer(8468, kserver.protocol)
udpserver.setServiceParent(application)

db = MongoClient().message_database
messages = db.messages
messages.ensure_index("timestamp", expireAfterSeconds=3*60)

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
        prefix = request.path.split("/")[-1]
        ts = request.args["timestamp"][0]
        utc = datetime.datetime.fromtimestamp(float(ts))
        length = len(prefix)
        min = prefix
        max = prefix
        for x in range(0, 96-length):
            min = min + "0"
            max = max + "1"
        max = hex(int(max, 2))[2:-1]
        min = hex(int(min, 2))[2:-1]
        log.msg("Getting key: %s" % prefix)
        ret = ""
        for post in messages.find(
                                {
                                    "$and":
                                        [
                                            {'timestamp': {"$gt": utc}},
                                            {"$and":
                                                 [
                                                     {"_id": {"$lt": ObjectId(max)}},
                                                     {"_id": {"$gt": ObjectId(min)}}
                                                 ]
                                            }
                                        ]
                                }):
            ret = ret + post.get("message") + " "
        if ret != "":
            respond(ret.encode("UTF-8"))
            return server.NOT_DONE_YET
        else:
            self.delayed_requests.append(request)
            #d = self.kserver.get(request.path.split('/')[-1])
            #d.addCallback(respond)
            return server.NOT_DONE_YET

    def render_POST(self, request):
        key = request.path.split('/')[-1]
        value = request.content.getvalue()
        post = {"_id": ObjectId(key),
                "message": value,
                "timestamp": datetime.datetime.utcnow()}
        messages.insert(post)
        self.incoming_posts.append(key)
        log.msg("Setting %s = %s" % (key, value))
        self.kserver.set(key, value)
        return value

    def processDelayedRequests(self):
        """
        Processes the delayed requests that did not have
        any data to return last time around.
        """
        # run through delayed requests
        for request in self.delayed_requests:
            reqkey = request.path.split("/")[-1]
            for postkey in self.incoming_posts:
                binpostkey = bin(int(postkey, 16))[2:]
                if binpostkey[:len(reqkey)] == reqkey:
                    try:
                        ret = messages.find_one({"_id": ObjectId(postkey)})
                        request.write(ret.get("message").encode("UTF-8"))
                        request.finish()
                    except:
                        # Connection was lost
                        print 'connection lost before complete.'
                    finally:
                        # Remove request from list
                        self.delayed_requests.remove(request)
        self.incoming_posts = []

website = server.Site(WebResource(kserver))
webserver = internet.TCPServer(8080, website)
webserver.setServiceParent(application)


# To test, you can set with:
# $> curl --data "hi there" http://localhost:8080/one
# and get with:
# $> curl http://localhost:8080/one
