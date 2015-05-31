__author__ = 'chris'

from bitcoin import *

from subspace.pyelliptic import *
from subspace import payload
from subspace.utils import digest

from pyelliptic.hash import hmac_sha256

class MessageEncoder(object):

    def __init__(self, recipient_pub, sender_priv, message, range):
        """
        Args:
            recipient_pub: a hex encoded compressed public key
            sender_priv: a hex encoded private key
            message: the message as  string
            range: the range the recipient's public key would fall within.
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
        pubkey = privkey_to_pubkey(sender_priv)
        pubkey_raw = changebase(pubkey[2:],16,256,minlen=64)
        pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]
        self.alice = ECC(curve="secp256k1", raw_privkey=self.privkey, pubkey=pubkey)
        self.bob = ECC(curve='secp256k1', pubkey=self.pubkey)
        self.shared_secret = self.alice.get_ecdh_key(self.pubkey)[:32]

    def split_and_encrypt(self):
        messages = []
        data = payload.MessageData()
        data.messageID = digest(os.urandom(32))
        data.sequence = 0
        data.senderKey = encode_pubkey(privkey_to_pubkey(self.privkey_hex), "hex_compressed")
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
        # We do some trivial brute forcing to get the hash of the message within the same range as the
        # recipient's public key
        messages = {}
        for ciphertext in ciphertexts:
            entropy = os.urandom(32).encode("hex")
            nonce = 0
            if self.range == 0:
                nonce_hash = digest(entropy + str(0))
                message_hash = hash160(ciphertext + nonce_hash)
                key = message_hash
            else:
                low = long(self.pubkey_hex[2:42], 16) - self.range / 4
                high = long(self.pubkey_hex[2:42], 16) + self.range / 4
                while True:
                    nonce_hash = digest(entropy + str(nonce))
                    message_hash = hash160(ciphertext + nonce_hash)
                    long_hash = long(message_hash, 16)
                    if low < long_hash < high:
                        key = message_hash
                        break
                    nonce += 1
            messages[key] = ciphertext + nonce_hash
        return messages

    def create_messages(self):
        ciphertexts = self.split_and_encrypt()
        messages = self.create_keys(ciphertexts)
        return messages

class MessageDecoder(object):

    def __init__(self, private_key, messageDic):
        self.messageDic = messageDic
        self.priv_bin = encode_privkey(private_key, "bin")
        pubkey = privkey_to_pubkey(private_key)
        pubkey_raw = changebase(pubkey[2:],16,256,minlen=64)
        pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]
        self.bob = ECC(curve="secp256k1", raw_privkey=self.priv_bin, pubkey=pubkey)

    def get_messages(self):
        # First try to decrypt all the messages
        messages = {}
        for k, v in self.messageDic.items():
            # Don't bother attempting to decrypt if the hash doesn't match
            if hash160(v[1]) == k:
                ciphertext = v[1][:len(v[1]) - 20]
                try:
                    messages[k] = self.bob.decrypt(ciphertext)
                except:
                    None

        # Parse each decrypted message and validate the hmac
        spayload = payload.SignedPayload()
        grouped_messages = {}
        for signed_payload in messages.values():
            spayload.ParseFromString(signed_payload)
            data = payload.MessageData()
            data.ParseFromString(spayload.serializedMessageData)

            sender_pub = data.senderKey
            pub = decode_pubkey(sender_pub, formt='hex_compressed')
            pubkey_hex = encode_pubkey(pub, formt="hex")
            pubkey_raw = changebase(pubkey_hex[2:],16,256,minlen=64)
            pubkey = '\x02\xca\x00 '+pubkey_raw[:32]+'\x00 '+pubkey_raw[32:]

            shared_secret = self.bob.get_ecdh_key(pubkey)[:32]
            hmac = hmac_sha256(shared_secret, spayload.serializedMessageData)
            # If the hmac is valid, group the messages by message ID
            if hmac == spayload.HMac:
                if data.messageID not in grouped_messages.keys():
                    grouped_messages[data.messageID] = [data]
                else:
                    mlist = grouped_messages[data.messageID]
                    mlist.append(data)
                    grouped_messages[data.messageID] = mlist
        # Run through the grouped messages and reconstruct the plaintext in the proper order
        reconstructed_messages = []
        for message_list in grouped_messages.values():
            m = {}
            full_message = ""
            for i in range(0, len(message_list)):
                for data in message_list:
                    if data.sequence == i:
                        full_message += data.unencryptedMessage
            m["sender"] = message_list[0].senderKey
            m["timestamp"] = message_list[0].timeStamp
            m["plaintext"] = full_message
            reconstructed_messages.append(m)

        return reconstructed_messages