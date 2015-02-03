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
import json
import calendar

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

'''This function is used to go from UTC to unix time with two decimal precision.'''
def unix_time(utc):
        epoch = datetime.datetime.utcfromtimestamp(0) - datetime.timedelta(0, 0, 10000)
        delta = utc - epoch
        return delta.total_seconds()

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
        utc = datetime.datetime.utcfromtimestamp(float(ts))
        length = len(prefix)
        min = prefix
        max = prefix
        for x in range(0, 96-length):
            min = min + "0"
            max = max + "1"
        max = hex(int(max, 2))[2:-1]
        min = hex(int(min, 2))[2:-1]
        if len(min) < 24:
            for x in range(0, 24-len(min)):
                min = "0" + min
        if len(max) < 24:
            for x in range(0, 24-len(max)):
                max = "0" + max
        log.msg("Getting key: %s" % prefix)
        dict = {}
        timestamp = datetime.datetime.utcfromtimestamp(float(0))
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

            if post.get("timestamp") > timestamp:
                timestamp = post.get("timestamp")
            dict[str(post.get("_id"))] = post.get("message")
        if bool(dict)==True:
            dict["timestamp"] = unix_time(post.get("timestamp"))
            respond(json.dumps(dict))
            return server.NOT_DONE_YET
        else:
            self.delayed_requests.append(request)
            #d = self.kserver.get(request.path.split('/')[-1])
            #d.addCallback(respond)
            return server.NOT_DONE_YET

    def render_POST(self, request):
        key = request.path.split('/')[-1]
        value = request.content.getvalue()
        print ObjectId(key)
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
        x = len(self.delayed_requests)
        for request in self.delayed_requests:
            x-=1
            reqkey = request.path.split("/")[-1]
            for postkey in self.incoming_posts:
                binpostkey = str(bin(int(postkey, 16))[2:])
                if len(binpostkey)<24:
                    for x in range(0, 24-len(binpostkey)):
                        binpostkey = "0" + binpostkey
                if binpostkey[:len(reqkey)] == reqkey:
                    try:
                        map = {}
                        ret = messages.find_one({"_id": ObjectId(postkey)})
                        map["timestamp"] = unix_time(ret.get("timestamp"))
                        map[str(ret.get("_id"))] = ret.get("message")
                        request.write(json.dumps(map))
                        request.finish()
                    except:
                        # Connection was lost
                        print 'connection lost before complete.'
                    finally:
                        # Remove request from list
                        self.delayed_requests.remove(request)
                if x==0:
                    self.incoming_posts.remove(postkey)

website = server.Site(WebResource(kserver))
webserver = internet.TCPServer(8080, website)
webserver.setServiceParent(application)


# To test, you can set with:
# $> curl --data "hi there" http://localhost:8080/one
# and get with:
# $> curl http://localhost:8080/one
