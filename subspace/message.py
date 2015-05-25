__author__ = 'chris'

from bitcoin import *

from subspace.pyelliptic import *
from subspace import payload
from subspace.utils import digest

from pyelliptic.hash import hmac_sha256

import binascii

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
        self.privkey_hex = sender_priv
        self.privkey = encode_privkey(sender_priv, "bin")
        self.message = message
        self.length = 0
        self.range = range
        self.alice = ECC(curve="secp256k1", raw_privkey=self.privkey)
        self.bob = ECC(curve='secp256k1', pubkey=self.pubkey)
        self.shared_secret = self.alice.get_ecdh_key(self.pubkey)[:32]

    def create_messages(self):
        messages = []
        data = payload.MessageData()
        data.messageID = digest(os.urandom(32))
        data.sequence = 0
        data.senderAddress = encode_pubkey(privkey_to_pubkey(self.privkey_hex), "hex_compressed")
        data.timeStamp = int(time.time())
        data.unencryptedMessage = self.message

        def pad():
            pad_len = 500 - len(data.SerializeToString())
            rand_pad = os.urandom(pad_len)
            data.pad = rand_pad
            excess = len(data.SerializeToString()) - 500
            data.pad = rand_pad[excess:]
            sign_message(data.SerializeToString())

        def sign_message(serialized_message):
            hmac = hmac_sha256(self.shared_secret, serialized_message)
            signed_payload = payload.SignedPayload()
            signed_payload.serializedMessageData = serialized_message
            signed_payload.HMac = hmac
            messages.append(self.alice.encrypt(signed_payload.SerializeToString(), self.pubkey))

        overage = 1
        while overage > 0:
            overage = len(data.SerializeToString()) - 500
            if overage < 0:
                pad()
            elif overage == 0:
                sign_message(data.SerializeToString())
            elif overage > 0:
                data.unencryptedMessage = self.message[:len(data.unencryptedMessage) - overage]
                sign_message(data.SerializeToString())
                self.message = self.message[len(data.unencryptedMessage):]
                data.unencryptedMessage = self.message
            data.sequence += 1
        return messages

    def create_keys(self, ciphertexts):
        messages = {}
        for ciphertext in ciphertexts:
            entropy = os.urandom(32).encode("hex")
            nonce = 0
            if self.range == 0:
                key = hash160(entropy)
            else:
                low = long(self.pubkey_hex[2:42], 16) - self.range / 2
                high = long(self.pubkey_hex[2:42], 16) + self.range / 2
                while True:
                    hash = hash160(entropy + str(nonce))
                    long_hash = long(hash, 16)
                    if low < long_hash < high:
                        key = hash
                        break
                    nonce += 1
            messages[key] = ciphertext
        return messages

    def get_messages(self):
        ciphertexts = self.create_messages()
        messages = self.create_keys(ciphertexts)
        return messages

class MessageDecoder(object):

    def __init__(self, private_key, kserver):
        self.messageDic = kserver.storage.get_all()
        self.privkey = private_key
        self.kserver = kserver


    def getMessages(self):
        priv_bin = encode_privkey(self.privkey, "bin")
        pubkey = privkey_to_pubkey(self.privkey)
        pubkey_raw = changebase(pubkey[2:],16,256,minlen=64)
        pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]
        bob = ECC(curve="secp256k1", raw_privkey=priv_bin, pubkey=pubkey)
        messages = {}
        for k, v in self.messageDic.items():
            ciphertext = v[1]
            try:
                messages[k] = bob.decrypt(binascii.unhexlify(ciphertext))
            except:
               None
        return messages