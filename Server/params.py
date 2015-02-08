__author__ = 'chris'
from config import Config

def get_options():
    options = {}
    f = file('Subspace.cfg')
    cfg = Config(f)
    if "useSSL" in cfg:
        options["useSSL"] = cfg.useSSL
    if "sslkey" in cfg:
        options["sslkey"] = cfg.sslkey
    if "sslcert" in cfg:
        options["sslcert"] = cfg.sslcert
    if "node" in cfg:
        options["node"] = cfg.node
    if "limit" in cfg:
        options["limit"] = cfg.limit
    if "storeall" in cfg:
        options["storeall"] = cfg.storeall
    if "storesome" in cfg:
        options["storesome"] = cfg.storesome
    if "ttl" in cfg:
        options["ttl"] = cfg.ttl
    return options

o = get_options()
print o["ttl"]