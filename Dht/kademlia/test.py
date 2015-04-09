__author__ = 'chris'
import hashlib
from array import array

x = hashlib.sha1("Test").digest()
if len(x) == 40:
    print "is true"
if len('bcdf8bfc1a5fa9e2a004283a9fdf660e051497b2') == 40:
    print True