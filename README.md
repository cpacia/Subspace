#Subspace
######P2P Anonymous Messaging Protocol

The protocol is currently under development. 

To install subspace:
```
git clone https://github.com/cpacia/Subspace.git
cd Subspace
./install.sh
```

To run a node:

```
subspace start
```

There is an echo server running at 17hDJEUfUvKHPhAptEnmVzK9yyYh948cNmKvTEH1Ua2extiP2RQ.
Send it a message using:

```
subspace send -k 17hDJEUfUvKHPhAptEnmVzK9yyYh948cNmKvTEH1Ua2extiP2RQ -m "Hello world!"
```

And check your received messages:

```
subspace getmessages
```
or
```
subspace getnew
```

######High Level Protocol Overview
Sending: Outgoing messages are inserted into the DHT using a key that falls within the same neighborhood as the recipeint's public key.

Receiving: By joining the DHT using your public key as your node ID, your neighboors will send you all the stored messages in your neighborhood. You attempt to decrypt each one to find messages intended for you.

By using this structure a node only needs to store a fraction of the overall messages in the network and recipients only need to download and attempt to decrypt a fraction of the overall message space. Still, it is not possible to tell who a message is intended for as it could have been sent to any one of approximately 2<sup>19</sup> public keys (depending on the number of nodes in the network). 

######TODO
1. Switch out the wire protocol from pickling to protobuf.
2. Add support for lightweight p2p clients that don't store messages and can outsource the iterative queries.
3. Explore using the DHT over TCP instead of/in addition to UDP to allow for better Tor support.
4. Maybe switch to 256 bit DHT.
5. Maybe encrypt the wire protocol. 
