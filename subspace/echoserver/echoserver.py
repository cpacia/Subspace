__author__ = 'chris'
import time
import daemon

from twisted.internet import reactor
from txjsonrpc.netstring.jsonrpc import Proxy

from bitcoin import *

def run_echoserver():
    def getNew():
        time.sleep(1)
        d = proxy.callRemote('getnew')
        d.addCallbacks(echo, printError)

    def echo(messages):
        for message in messages:
            value = message["plaintext"]
            key = message["sender"]
            d = proxy.callRemote('send', key, value)
        getNew()

    def printError(error):
        print 'error', error

    proxy = Proxy('127.0.0.1', 7080)
    running = False
    getNew()
    reactor.run()

with daemon.DaemonContext():
    run_echoserver()
