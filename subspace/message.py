__author__ = 'chris'

from bitcoin import *
from subspace.pyelliptic import *

"""
TODO: clean up this module. Plaintext will be serialized with protobuf including timestamp, sender pubkey,
sender name, and hmac. Create a few methods for returning various parts of the object such as the serialized plaintext.
And add some comments so people know what's going on.

"""

class MessageEncoder(object):

    def __init__(self, recipient_pub, sender_priv, message, range):
        """
        Args:
            recipient_pub: a hex encoded compressed public key
            sender_priv: a hex encoded private key
            message: the message as  string
        """

        pub = decode_pubkey(recipient_pub, formt='hex_compressed')
        pubkey_hex = encode_pubkey(pub, formt="hex")
        pubkey_raw = changebase(pubkey_hex[2:],16,256,minlen=64)
        self.pubkey_hex = recipient_pub
        self.pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]
        self.privkey = encode_privkey(sender_priv, "bin")
        self.message = message
        self.ciphertext = ""
        self.length = 0
        self.range = range

    def getblocks(self):
        alice = ECC(curve="secp256k1", raw_privkey=self.privkey)
        bob = ECC(curve='secp256k1', pubkey=self.pubkey)
        self.ciphertext = binascii.hexlify(alice.encrypt(str(self.message), bob.get_pubkey()))
        self.length = len(self.ciphertext) / 2
        return self.create_header(self.split(self.ciphertext))

    def split(self, ciphertext):
        chunks = [ciphertext[i:i+946] for i in range(0, len(ciphertext), 946)]
        for x in range(0, len(chunks)):
            if len(chunks[x]) < 946:
                n = (946 - len(chunks[x])) / 2
                pad = os.urandom(n).encode('hex')
                chunks[x] += pad
        blocks = {}
        for chunk in chunks:
            hash = hash160(chunk)
            blocks[hash] = chunk
        return blocks

    def create_header(self, blocks):
        pad = os.urandom(20).encode("hex")
        num_blocks = hex(len(blocks))[2:].zfill(2)
        len_ciphtertext = hex(self.length)[2:].zfill(4)
        plaintext = num_blocks + len_ciphtertext
        for key in blocks.keys():
            plaintext += key
        for i in range(0, 8-len(blocks)):
            plaintext += pad
        alice = ECC(privkey=self.privkey, curve="secp256k1")
        bob = ECC(curve='secp256k1', pubkey=self.pubkey)
        ciphertext = binascii.hexlify(alice.encrypt(plaintext, bob.get_pubkey()))
        entropy = os.urandom(32).encode("hex")
        nonce = 0

        if self.range == 0:
            ciphertext = ciphertext + hashlib.sha512(entropy + str(nonce)).hexdigest()[:50]
            blocks[hash160(ciphertext)] = ciphertext
            return blocks
        else:
            low = long(self.pubkey_hex[:40], 16) - self.range / 2
            high = long(self.pubkey_hex[:40], 16) + self.range / 2

            while True:
                c = ciphertext + hashlib.sha512(entropy + str(nonce)).hexdigest()[:50]
                hash = long(hash160(c), 16)
                if low < hash < high:
                    ciphertext = c
                    break
                nonce += 1
            blocks[hash160(ciphertext)] = ciphertext
            return blocks

class MessageDecoder(object):

    def __init__(self, private_key, kserver):
        self.messageDic = kserver.storage.get_all()
        self.privkey = private_key
        self.kserver = kserver


    def getMessages(self):
        headers = {}
        priv_bin = encode_privkey(self.privkey, "bin")
        pubkey = privkey_to_pubkey(self.privkey)
        pubkey_raw = changebase(pubkey[2:],16,256,minlen=64)
        pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]
        bob = ECC(curve="secp256k1", raw_privkey=priv_bin, pubkey=pubkey)
        for k, v in self.messageDic.items():
            full_message = v[1]
            ciphertext = full_message[:896]
            try:
                headers[k] = bob.decrypt(binascii.unhexlify(ciphertext))
            except:
               None
        messages = []
        for k, v in headers.iteritems():
            num_blocks = int(v[:2], 16)
            len_c = int(v[2:6], 16) * 2
            n = 6
            blockids = []
            for i in range(0, num_blocks):
                blockids.append(v[n: n+40])
                n = n + 40
            cipherblocks = []
            for blockid in blockids:
                if self.messageDic.has_key(blockid):
                    cipherblocks.append(self.messageDic[blockid])
                else:
                    # this needs to be deffered
                    cipherblocks.append(self.kserver.get(blockid))
            ciphertext = ""
            for block in cipherblocks:
                ciphertext += block[1]
            ciphertext = ciphertext[:len_c]
            plaintext = bob.decrypt(binascii.unhexlify(ciphertext))
            dict = {}
            dict[k] = plaintext
            messages.append(dict)
        return messages