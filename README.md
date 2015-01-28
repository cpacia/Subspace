#Subspace
######P2P Anonymous Messaging Protocol

###Abstract 
A sketch of a store and forward network that can be used as an alternate communication channel for Bitcoin payments. Over the channel we should be able to anonymously send Bitcoin payments, payment requests, and even arbitrary messages. The idea is loosely based on Mike Hearn’s article [ECDH in the Payment Protocol](https://medium.com/@octskyward/ecdh-in-the-payment-protocol-cb2f81962c1b).

###Motivation

#####Privately sending and receiving payments from a lightweight wallet is hard.

Bloom filters and prefix filters, even when used correctly, offer very brittle privacy. Merging coins, in addition to associating previously unassociated transactions, will identify which addresses are in the filter if coinjoin is not used. To use them correctly requires somewhat awkward hacks that involve creating filters much larger than often needed and connecting to multiple peers/servers if additional filters are created. At present no wallet implements filtering correctly.

Sending transactions directly to the recipient over a different channel removes the need to download transactions from the network. Filtering can still be used to query for block inclusion, but isn’t a requirement as this can be done on a one off basis from a different Tor identity each time.

#####Users should be able to attach a private message for the recipient to the transaction.

Currently, short memos can be attached to transactions, but they are publically visible for everyone to see. By using an alternative channel we use can use the ‘memo’ field in the BIP70 payment message to attach private messages to transactions.

#####An alternate communication channel would enable person-to-person payment requests.

BIP70 is limited by several factors: To automatically download a payment request from another user, the sender’s computer needs to remain online (and accepting incoming connections) at all times. This is not feasible for end user wallets.

Payment requests can be manually exported from the wallet and sent over email (or another channel) with the recipient manually importing the file into the wallet, but this clunky and offers poor UX.

Additionally, the payment request model is currently a “pull” model. The ability to “push” a payment request to an end user would enable merchants to bill end users for monthly services.

Utilizing an alternative channel would make payment requests seamless.

#####The communication would finally enable P2P coinjoin transactions.

At present there are only two coinjoin implementations and both are server based. Segregating coinjoin transactions by server means the pool of available participants is smaller than it need be leading to less robust mixing, longer mix times, and unnecessary centralization. A p2p communication channel could finally enable a single coinjoin protocol for all wallets.

#####The channel could be used for generic anonymous messaging.

Given the encrypted nature of the channel, it won’t be possible to limit messages to just payments and payment requests. As such, the channel could be extended to enable anonymous messaging between clients.

###Why not Bitmessage?

Bitmessage seems like it would be ideal for this purpose, but there are a couple drawbacks. At present all nodes need to download all messages and sort through them. This creates a large overhead that is not appropriate for lightweight clients. There are proposals to add lite client support to Bitmessage, but they have not yet been implemented.

More importantly the proof of work severely harms UX. The white paper suggests targeting a POW time of 4 minutes on an average computer (it currently takes around 1.5 to 2 minutes depending on the system). Compared to current sub-second bitcoin transaction times, Bitmessage would be a major step backwards for the user experience.

Finally it’s a bit of a curious choice to require all nodes (or all nodes in a stream) to store all messages. Unlike Bitcoin, there is no consensus mechanism at play which requires all nodes to store all messages.

Doing so precludes (absent breakthroughs in micro payments) applying market mechanisms to ration scarce resources (disk space) and requires a network run by volunteers using POW as an awkward hack to ration disk space.

###Goals

Ideally the store and forward protocol would have the following characteristics:

1. All messages should be encrypted with only the recipient able to decrypt.

2. The sender and recipient should be hidden from passive observers.

3. The protocol should be able to scale to handle an extremely large number of messages.

4. The protocol should be able to prevent attackers from flooding the network with arbitrary messages.

5. Anti-Spam mechanisms should not noticeably harm UX.

6. The protocol should support lightweight queries and user-defined anonymity sets.

7. The protocol should support market mechanisms (where needed) to ration disk space.

###Protocol

#####Network architecture.

Similar to Mike Hearn’s proposal, the protocol will be run on a network of servers. A user can upload a message to a server using HTTP POST and retrieve a message using a long polling GET.

Rather than using an email-like network however, each server running this protocol will be a node in a distributed hash table (DHT) allowing the servers to query each other for messages they do not have. The following would be a typical message flow:

```
Alice picks a reliable server (server A) and POSTs an encrypted message to it.

Server A inserts the message’s key into the DHT***

Bob makes a GET request from server B.

Server B queries the DHT to find the IP address of the server(s) storing the message.

Server B connects to Server A and downloads the message.

Server B forwards the message to Bob.
```

In this protocol anyone will be able to run a server. Obviously, clients will need to pick a reliable server with the most uptime. Should a server go offline, the message would be undeliverable and the user would need to resend using a different server.

There would be nothing stopping wallets themselves from being a node in the DHT, but doing so would require them to host their own messages, accept incoming connections, remain permanently online, and likely operate as a hidden service to remain private. Hence, why a network of servers is more attractive.

***In order for a server to serve a message to a client as soon as it’s inserted, the server needs to be made aware of DHT insertions by other servers. The topology of the Kademlia overlay makes for effient broadcast messaging. Nodes can relay messages to one node per sub-tree who in turn relay it to their own sub-trees. Likely more than one relay node per sub-tree would need to recieve the message to insulate from network errors. 

#####Encryption

Messages would be encrypted using a shared secret derived using ECDH and the key found in the user’s stealth address.

Authentication of keys is beyond the scope of this protocol and would be left to wallet developers to figure out what works best.

#####Message tagging and anonymity sets

Each message would be tagged with a 160 bit number derived in part from the stealth address. The tag would also serve as the ‘key’ used for DHT insertions.

Stealth addresses contain a 32 bit ‘prefix’ and one byte for the ‘length’. The message recipient uses the length parameter to define their anonymity set.

For example, if a length of zero is used, the sender would use a random number as the tag and the recipient would download every message from the network and attempt to decrypt each one using his key. Very similar to how Bitmessage works.

If a length of one is used, the sender would set the first bit in the message tag to the same bit (zero or one) of the stealth address prefix. The remaining bits in the tag would be random.

The recipient would give the server his prefix and the server would return all the messages with tags starting with the same bit. Like before, the user would attempt to decrypt each message message. This would effectively reduce the amount of messages the user needs to sort through by 50% (while also decreasing the anonymity set by 50%).

Each additional bit of length reduces the number of returned messages (and anonymity set) by another 50%. A prefix of 4 bits, for example, would return about 6.25% of all messages in the network.

A length of 32 bits would imply the user would only download her messages, minimizing bandwidth but allowing passive observes to know which (encrypted) messages were intended for her.

#####Scalability

The network described here would scale much better than Bitmessage as the messages would not be stored by the entire network, but rather only by the server to which they were uploaded (and possibly other servers which voluntarily chose to download and store some messages locally for quicker access).

Furthermore, as the on and off ramps to the network, the servers could (if needed) charge a small fee for their services, incentivizing people to start up additional servers when the load gets high and rationing disk space by price rather than proof of work.

#####Anti-spam

Because messages are not stored by each node (like Bitmessage) we do not need to use a proof-of-work to prevent the network from being overrun with spam messages. Instead, it becomes the responsibility of each server to implement their own anti-spam measures and servers can employ the traditional tactics used by email servers (such as quotas).

Implementing quotas without harming privacy is tricky but doable. The following is one example of how a sender quota system could be done without compromising privacy:

```
A user creates an account with a server by filling out a captcha and receiving an account number.

The client logs in with its account number sends a bunch of blinded tokens to the server.

For each message under the quota, the server blind signs a token and returns it to the client.

When sending a message, the client attaches an unblinded token.

Server verifies its signature and inserts the message into the DHT.
```

In this example the server can tell that the message was sent by someone who is under their quota, but it doesn’t know who.

Alternatively, the server could eliminate the quota and charge a fee per N tokens to ration by price.

#####DDoS

The network of servers would be subject to classical denial of service attacks. However, it will be the responsibility of each server to mitigate against denial of service attacks, not the protocol. Servers with low latency, high up-time, and robust anti-DDoS measures will attract more traffic, which may be a competitive advantage if the server is run on a for-profit model (i.e. leasing storage space).

###Summary

1. The above protocol should satisfy all of ours goals.

2. All messages are encrypted with only the sender and recipient able to decrypt.

3. The sender’s identity is not attached to the message at all and the only a user-defined number of bits from the recipient address is attached to the message. In other words, no meaningful data is available to passive observers.

4. By splitting the message load among servers (possibly with a financial incentive), the network can scale to handle a very large number of messages.

5. Because the network is server based, servers can implement traditional anti-spam measures without harming user experience.

6. The protocol supports lightweight queries allowing the user to make the anonymity set/bandwidth tradeoff.

7. The protocol supports market based rationing mechanisms where necessary.
