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
twistd -noy server.py
```

Client Setup
---------------------
The client uses Java 8 with JavaFX and Apache Maven for building the project. You should be able to use Intellij or Eclipse with no problem. In a few days the POC client will be ready and I'll post a jar and debian and Mac packages. 

There is some kind of formatting issue in my SSL certificate for bitcoinauthenticator.org. From what I understand, it doesn't effect the handshake and most browsers just ignore it. Unfortunately, java does not and it will throw a handshake exception. What that means is you need to manually import the certificate into the JRE.

1. Visit bitcoinauthenticator.org and download the cert. (Usually by clicking a lock icon near the URL and selecting export in the options).

2. Import it into the JRE:
```
cd /usr/lib/jvm/java-8-oracle/bin
```

```
keytool -importcert -file /path/to/certificate/ -keystore /usr/lib/jvm/java-8-oracle/jre/lib/security/cacerts -alias "Alias" -storepass changeit
```

### Running
When you create a new address make sure you want to test it with the server instance running on localhost, make sure you select "localhost" when creating an address. Otherwise it will use the server running on bitcoinauthenticator.org.

Apologies for the lack of comments in the code. I will go back and add them in a couple days. 

TODO
---------------------
* Create Chatroom UI


