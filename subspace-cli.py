__author__ = 'chris'
import pyjsonrpc
import sys
import argparse

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
    getmessages      Returns a list of your messages in json format
    getpubkey        Returns your node's public encryption key
    send             Sends a message to the given public key
    start            Start the Subspace daemon
    stop             Close subspace and disconnect
''')
        parser.add_argument('command', help='Execute the given command')
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
        parser.add_argument('-k', '--key', required=True, help="Recipient's public key")
        parser.add_argument('-m', '--message', required=True, help="The unencrypted message to send (will be encrypted)")
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
        parser.add_argument('-b', '--base58', help="Returns the key in base58check format")
        args = parser.parse_args(sys.argv[2:])
        print http_client.getpubkey()

if __name__ == '__main__':
    Parser()
