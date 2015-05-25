__author__ = 'chris'

from subspace.message import MessageEncoder
from bitcoin import *

import binascii

nodes = 1000
low = long("0000000000000000000000000000000000000000", 16) / nodes * 3
high = low + (long("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16) / nodes * 20)
range = high - low
message = "Literally, sustainable development refers to maintaining development over time, although by the early 1990s, more than 70 definitions of sustainable development were in circulation, definitions that are important, despite their number, because they are the basis on which the means for achieving sustainable development in the future can be built.Literally, sustainable development refers to maintaining development over time, although by the early 1990s, more than 70 definitions of sustainable development were in circulation, definitions that are important, despite their number, because they are the basis on which the means for achieving sustainable development in the future can be built.Literally, sustainable development refers to maintaining development over time, although by the early 1990s, more than 70 definitions of sustainable development were in circulation, definitions that are important, despite their number, because they are the basis on which the means for achieving sustainable development in the future can be built.Literally, sustainable development refers to maintaining development over time, although by the early 1990s, more than 70 definitions of sustainable development were in circulation, definitions that are important, despite their number, because they are the basis on which the means for achieving sustainable development in the future can be built."

alice = random_key()
bob = encode_pubkey(privkey_to_pubkey(random_key()), "hex_compressed")

enc = MessageEncoder(bob, alice, message, range)
for m in enc.get_messages().iteritems():
    print m