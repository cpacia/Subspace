__author__ = 'chris'
import time
import sys
from daemon import Daemon

from twisted.internet import reactor
from txjsonrpc.netstring.jsonrpc import Proxy

def run_echoserver():
    def getNew():
        time.sleep(1)
        d = proxy.callRemote('getnew')
        d.addCallbacks(echo, printError)

    def echo(messages):
        def printResp(resp):
            print resp
        for message in messages:
            value = message["plaintext"]
            key = message["sender"]
            print "Received message, echoing..."
            d = proxy.callRemote('send', key, "Echo: " + str(value))
            d.addCallback(printResp)
        getNew()

    def printError(error):
        print 'error', error
        getNew()

    proxy = Proxy('127.0.0.1', 7080)
    getNew()
    reactor.run()

class EchoDaemon(Daemon):
        def run(self):
            run_echoserver()

if __name__ == "__main__":
        daemon = EchoDaemon('/tmp/echodaemon.pid')
        if len(sys.argv) == 2:
                if 'start' == sys.argv[1]:
                        daemon.start()
                elif 'stop' == sys.argv[1]:
                        daemon.stop()
                elif 'restart' == sys.argv[1]:
                        daemon.restart()
                else:
                        print "Unknown command"
                        sys.exit(2)
                sys.exit(0)
        else:
                print "usage: %s start|stop|restart" % sys.argv[0]
                sys.exit(2)
