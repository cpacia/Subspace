Proof-of-Concept Client and Server
=====================

Server Setup
---------------------
A server is running on bitcoinauthenticator.org which you can use with the Client. Messages are set to expire after three mintues for now ― enough to test it. To run a server on localhost you will need the following:

1. MongoDB. Follow the [installation instructions](http://docs.mongodb.org/manual/installation/) for your OS.
2. Python [Twisted](https://pypi.python.org/pypi/Twisted).  You may need to install python-dev and some other packages to get it running.
3. Kademlia. A [fork](https://github.com/BitcoinAuthenticator/Subspace/tree/master/Dht) can be found in this repo. To get the servers talking to each other we will need to modify this fork, that's why it's here, but I haven't started it yet. Run setup.py.

### Running
Start mongodb 
```
sudo service mongod start
```

Start the subspace server
```
twistd -noy Server.py
```

Client Setup
---------------------
The client uses Java 8 with JavaFX and Apache Maven for building the project. You should be able to use Intellij or Eclipse with no problem. In a few days the POC client will be ready and I'll post a jar and debian and Mac packages. 

### Running
When you create a new address make sure you select 'localhost' as the upload node so it will upload messages to the server running on your system. Other than that it will work automatically. 

Apologies for the lack of comments in the code. I will go back and add them in a couple days. 

TODO
---------------------
* Create Email UI
* Create Chatroom UI
* Save email and chatroom messages to and load from disk. 
* Get the server running on bitcoinauthenticator.org and integrate SSL. 
* Hook up Tor for sending/receiving messages.


