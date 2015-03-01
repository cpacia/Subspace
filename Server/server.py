from twisted.application import service, internet
from twisted.python.log import ILogObserver
from twisted.python import log
from twisted.internet import reactor, task, ssl
from twisted.web import resource, server
from twisted.web.resource import NoResource

from pymongo import MongoClient
import gridfs

import sys, os
sys.path.append(os.path.dirname(__file__))
from kademlia.network import Server
from kademlia import log
import datetime
from bson.objectid import ObjectId
import json
import calendar
import params

#start mongodb -> sudo service mongod start

options = params.get_options()
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
fs = gridfs.GridFS(db)
if "ttl" in options:
    messages.ensure_index("timestamp", expireAfterSeconds=options["ttl"])
else:
    messages.ensure_index("timestamp", expireAfterSeconds=604800)

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
        if "timestamp" not in request.args:
            u = request.path.split("/")[-1]
            respond(fs.get_last_version(user=u).read())
            return server.NOT_DONE_YET
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

        if 'file' in request.args:
            u = request.args["user"]
            print request.args["file"][0]
            fs.put(request.args["file"][0], user=u)
            return "File uploaded successfully"
        key = request.path.split('/')[-1]
        value = request.content.getvalue()
        post = {"_id": ObjectId(key),
                 "message": value,
                "timestamp": datetime.datetime.utcnow()}
        if db.command("dbstats").get("dataSize") < options["limit"] and len(value) < 100000:
            messages.insert(post)
            self.incoming_posts.append(key)
            log.msg("Setting %s = %s" % (key, value))
            self.kserver.set(key, value)
        else:
            value = "Post rejected: Message is either too large or the server is full"
        return value


    def processDelayedRequests(self):
        """
        Processes the delayed requests that did not have
        any data to return last time around.
        """
        def hextobin(hexval):
            thelen = len(hexval)*4
            binval = bin(int(hexval, 16))[2:]
            while ((len(binval)) < thelen):
                binval = '0' + binval
            return binval
        # run through delayed requests
        x = len(self.delayed_requests)
        for request in self.delayed_requests:
            x-=1
            reqkey = request.path.split("/")[-1]
            for postkey in self.incoming_posts:
                binpostkey = hextobin(postkey)
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
if "useSSL" in options:
    webserver = internet.SSLServer(8335, website, ssl.DefaultOpenSSLContextFactory(
            options["sslkey"], options["sslcert"]))
else:
    webserver = internet.TCPServer(8080, website)
webserver.setServiceParent(application)


# To test, you can set with:
# $> curl --data <cipherText> http://localhost:8335/12bytekey
# and get with:
# $> curl http://localhost:8335/prefix?timestamp=
