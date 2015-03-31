import time
from itertools import izip
from itertools import imap
from itertools import takewhile
import operator
from collections import OrderedDict

from zope.interface import implements
from zope.interface import Interface


class IStorage(Interface):
    """
    Local storage for this node.
    """

    def __setitem__(key, value):
        """
        Set a key to the given value.
        """

    def __getitem__(key):
        """
        Get the given key.  If item doesn't exist, raises C{KeyError}
        """

    def get(key, default=None):
        """
        Get given key.  If not found, return default.
        """

    def iteritemsOlderThan(secondsOld):
        """
        Return the an iterator over (key, value) tuples for items older than the given secondsOld.
        """

    def iteritems():
        """
        Get the iterator for this storage, should yield tuple of (key, value)
        """


class ForgetfulStorage(object):
    implements(IStorage)

    def __init__(self, ttl=604800):
        """
        By default, max age is a week.
        """
        self.data = OrderedDict()
        self.ttl = ttl

    def __setitem__(self, key, value):
        if key in self.data:
            del self.data[key]
        self.data[key] = (time.time(), value)
        self.cull()

    def cull(self):
        for k, v in self.iteritemsOlderThan(self.ttl):
            self.data.popitem(last=False)

    def get(self, key, default=None):
        self.cull()
        if key in self.data:
            return self[key]
        return default

    def get_range(self, prefix, default=None):
        self.cull()
        results = []
        length = len(prefix)
        min = prefix
        max = prefix
        for x in range(0, 160-length):
            min += "0"
            max += "1"
        max = hex(int(max, 2))[2:-1]
        min = hex(int(min, 2))[2:-1]
        if len(min) < 40:
            for x in range(0, 40-len(min)):
                min = "0" + min
        if len(max) < 40:
            for x in range(0, 40-len(max)):
                max = "0" + max
        for key in self.data:
            if max > key > min:
                results.append(self[key])
        if len(results) is not 0:
            return results
        else:
            return default

    def __getitem__(self, key):
        self.cull()
        return self.data[key][1]

    def __iter__(self):
        self.cull()
        return iter(self.data)

    def __repr__(self):
        self.cull()
        return repr(self.data)

    def iteritemsOlderThan(self, secondsOld):
        minBirthday = time.time() - secondsOld
        zipped = self._tripleIterable()
        matches = takewhile(lambda r: minBirthday >= r[1], zipped)
        return imap(operator.itemgetter(0, 2), matches)

    def _tripleIterable(self):
        ikeys = self.data.iterkeys()
        ibirthday = imap(operator.itemgetter(0), self.data.itervalues())
        ivalues = imap(operator.itemgetter(1), self.data.itervalues())
        return izip(ikeys, ibirthday, ivalues)

    def iteritems(self):
        self.cull()
        ikeys = self.data.iterkeys()
        ivalues = imap(operator.itemgetter(1), self.data.itervalues())
        return izip(ikeys, ivalues)
