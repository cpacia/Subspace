__author__ = 'chris'
import pyjsonrpc
import sys, os
import argparse

from twisted.internet import reactor
from txjsonrpc.netstring.jsonrpc import Proxy

http_client = pyjsonrpc.HttpClient(
    url = "http://localhost:8336",
    username = "Username",
    password = "Password"
)

class Parser(object):

    def __init__(self):
        parser = argparse.ArgumentParser(
            description='Subspace v0.1',
            usage='''
    subspace <command> [<args>]
    subspace <command> --help

commands:
    getmessages      returns a list of your messages in json format
    getpubkey        returns your node's public encryption key
    send             sends a message to the given public key
    start            start the Subspace daemon
    stop             close subspace and disconnect
''')
        parser.add_argument('command', help='Execute the given command')
        parser.add_argument('-n', '--noisy', action='store_true', help="show log output")
        args = parser.parse_args(sys.argv[1:2])
        if not hasattr(self, args.command):
            parser.print_help()
            exit(1)
        getattr(self, args.command)()

    def send(self):
        parser = argparse.ArgumentParser(
            description="Send a message to the recipient's public key",
            usage='''usage:
    subspace send [-k PUBLIC KEY] [-m MESSAGE]''')
        parser.add_argument('-k', '--key', required=True, help="recipient's public key")
        parser.add_argument('-m', '--message', required=True,
                            help="the unencrypted message to send (will be encrypted)",
                            nargs='+')
        args = parser.parse_args(sys.argv[2:])
        http_client.send(args.key, args.message)
        print "Message to %s sent successfully" % args.key

    def getmessages(self):
        parser = argparse.ArgumentParser(
            description='Returns a list of your messages in json format',
            usage='''usage:
    subspace getmessages''')
        args = parser.parse_args(sys.argv[2:])
        print http_client.getmessages()

    def getpubkey(self):
        parser = argparse.ArgumentParser(
            description="Returns your node's public encryption key",
            usage='''usage:
    subspace getpubkey''')
        parser.add_argument('-b', '--base58', help="returns the key in base58check format")
        args = parser.parse_args(sys.argv[2:])
        print http_client.getpubkey()

Parser()
