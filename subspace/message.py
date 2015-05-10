__author__ = 'chris'

from bitcoin import main
from subspace.pyelliptic import arithmetic

import subspace.pyelliptic
import binascii
import os
import hashlib

class MessageBuilder(object):

    def __init__(self, recipient_pub, sender_priv, message, range):
        """
        Args:
            recipient_pub: a hex encoded compressed public key
            sender_priv: a hex encoded private key
            message: the message as  string
        """

        pub = main.decode_pubkey(recipient_pub, formt='hex_compressed')
        pubkey_hex = main.encode_pubkey(pub, formt="hex")
        pubkey_raw = arithmetic.changebase(pubkey_hex[2:],16,256,minlen=64)
        self.pubkey_hex = recipient_pub
        self.pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]
        self.privkey = sender_priv
        self.message = message
        self.ciphertext = ""
        self.length = 0
        self.range = range

    def encrypt(self):
        alice = subspace.pyelliptic.ECC(privkey=self.privkey)
        bob = subspace.pyelliptic.ECC(curve='secp256k1', pubkey=self.pubkey)
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
            hash = main.hash160(chunk)
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
        alice = subspace.pyelliptic.ECC(privkey=self.privkey)
        bob = subspace.pyelliptic.ECC(curve='secp256k1', pubkey=self.pubkey)
        ciphertext = binascii.hexlify(alice.encrypt(plaintext, bob.get_pubkey()))
        entropy = os.urandom(32).encode("hex")
        nonce = 0

        if self.range == 0:
            ciphertext = ciphertext + hashlib.sha512(entropy + str(nonce)).hexdigest()[:50]
            blocks[main.hash160(ciphertext)] = ciphertext
            return blocks
        else:
            low = long(self.pubkey_hex[:40], 16) - range / 2
            high = long(self.pubkey_hex[:40], 16) + range / 2

            while True:
                c = ciphertext + hashlib.sha512(entropy + str(nonce)).hexdigest()[:50]
                hash = long(main.hash160(c), 16)
                if low < hash < high:
                    ciphertext = c
                    break
                nonce += 1
            blocks[main.hash160(ciphertext)] = ciphertext
            return blocks

