__author__ = 'chris'

import argparse
import string
import pickle
import json

from twisted.internet import reactor
from txjsonrpc.netstring.jsonrpc import Proxy
from os.path import expanduser
from bitcoin import *

datafolder = expanduser("~") + "/.subspace/"
if os.path.isfile(datafolder + 'keys.pickle'):
    privkey = pickle.load(open(datafolder + "keys.pickle", "rb"))

def doContinue(value):
    pass

def printValue(value):
    print json.dumps(value, indent=4)
    reactor.stop()

def printError(error):
    print 'error', error
    reactor.stop()

class Parser(object):

    def __init__(self, proxy):
        parser = argparse.ArgumentParser(
            description='Subspace v0.2',
            usage='''
    subspace <command> [<args>]
    subspace <command> --help

commands:
    getinfo          returns an object containing various state info
    getmessages      returns a list of your messages in json format
    getnew           returns messages that have not been previously returned by this command
    getprivkey       returns your private encryption key
    getpubkey        returns your node's public encryption key
    send             sends a message to the given public key
    start            start the subspace daemon
    stop             close subspace and disconnect
''')
        parser.add_argument('command', help='Execute the given command')
        parser.add_argument('-n', '--noisy', action='store_true', help="show log output")
        args = parser.parse_args(sys.argv[1:2])
        if not hasattr(self, args.command):
            parser.print_help()
            exit(1)
        getattr(self, args.command)()
        self.proxy = proxy

    def send(self):
        parser = argparse.ArgumentParser(
            description="Send a message to the recipient's public key",
            usage='''usage:
    subspace send [-k PUBLIC KEY] [-m MESSAGE]''')
        parser.add_argument('-k', '--key', required=True, help="recipient's public key")
        parser.add_argument('-m', '--message', required=True,
                            help="the unencrypted message to send (will be encrypted)",
                            nargs='+')
        parser.add_argument('-d', '--dontstore', action='store_true', help="sends to an online recipient without storing on the network")
        args = parser.parse_args(sys.argv[2:])
        key = args.key
        if len(args.key) != 66 or all(c in string.hexdigits for c in args.key) is not True:
            try:
                key = b58check_to_hex(args.key)
            except:
                print "Invalid key. Enter a 33 byte public key in either hexadecimal for base58check format."
                return
        if args.dontstore:
            d = proxy.callRemote('send', key, args.message, False)
        else:
            d = proxy.callRemote('send', key, args.message)
        d.addCallbacks(printValue, printError)
        reactor.run()

    def getmessages(self):
        parser = argparse.ArgumentParser(
            description='Returns a list of your messages in json format',
            usage='''usage:
    subspace getmessages''')
        args = parser.parse_args(sys.argv[2:])
        d = proxy.callRemote('getmessages')
        d.addCallbacks(printValue, printError)
        reactor.run()

    def getprivkey(self):
        def printKey(key):
            if args.base58:
                print encode_privkey(key, "wif")
            else:
                print key
            reactor.stop()
        parser = argparse.ArgumentParser(
            description="Returns your private encryption key",
            usage='''usage:
    subspace getprivkey''')
        parser.add_argument('-b', '--base58', action='store_true', help="returns the key in base58check format")
        args = parser.parse_args(sys.argv[2:])
        d = proxy.callRemote('getprivkey')
        d.addCallbacks(printKey, printError)
        reactor.run()

    def getpubkey(self):
        def printKey(key):
            if args.base58:
                print hex_to_b58check(key, 0)
            else:
                print key
            reactor.stop()
        parser = argparse.ArgumentParser(
            description="Returns your node's public encryption key",
            usage='''usage:
    subspace getpubkey''')
        parser.add_argument('-b', '--base58', action='store_true', help="returns the key in base58check format")
        args = parser.parse_args(sys.argv[2:])
        d = proxy.callRemote('getpubkey')
        d.addCallbacks(printKey, printError)
        reactor.run()

    def getnew(self):
        parser = argparse.ArgumentParser(
            description="Returns messages that have not previously been returned by this command",
            usage='''usage:
    subspace getnew''')
        args = parser.parse_args(sys.argv[2:])
        d = proxy.callRemote('getnew')
        d.addCallbacks(printValue, printError)
        reactor.run()

    def getinfo(self):
        parser = argparse.ArgumentParser(
            description="Returns an object containing various state info",
            usage='''usage:
    subspace getinfo''')
        args = parser.parse_args(sys.argv[2:])
        d = proxy.callRemote('getinfo')
        d.addCallbacks(printValue, printError)
        reactor.run()

proxy = Proxy('127.0.0.1', 8334)
Parser(proxy)
