__author__ = 'chris'
from pymongo import MongoClient
import time
from bson.objectid import ObjectId
import binascii

db = MongoClient().message_database
messages = db.messages

def post(key, value):
    post = {"_id" : ObjectId(key),
                "message": value,
                "timestamp": time.time()}
    messages.insert(post)

def get(prefix, time):
    length = len(prefix)
    min = prefix
    max = prefix
    for x in range(0, 96-length):
        min = min + "0"
        max = max + "1"

    print hex(int(min, 2))[2:-1], hex(int(max, 2))[2:-1]

    for post in messages.find(
                                {
                                    "$and":
                                        [
                                            {'timestamp': {"$gt": time}},
                                            {"$and":
                                                 [
                                                     {"_id": {"$lt": ObjectId(hex(int(max, 2))[2:-1])}},
                                                     {"_id": {"$gt": ObjectId(hex(int(min, 2))[2:-1])}}
                                                 ]
                                            }
                                        ]
                                }):
        print post

def remove(key):
    messages.remove({"_id": ObjectId(key)})
    ret = messages.find_one({"_id": ObjectId(key)})
    if ret == None:
        print "Successfully removed " + key + " from the database"
    else:
        print "Failed to remove " + key + " from the database"

remove("aabeecc57701e15b3cbb681f")
remove("bbbeecc57701e15b3cbb681e")
post("bbbeecc57701e15b3cbb681e", "This is a secret message")
post("aabeecc57701e15b3cbb681f", "Secret message 2")
get("010", 0322179671.37)

binpostkey = bin(int("b943a12345", 16))[2:]
reqkey = "101"

print hex(int('101', 2))

print binpostkey
print binpostkey[:len(reqkey)] == reqkey