__author__ = 'chris'
x = [7, 7, 2, 9, 1]
ret = []
for val in x:
    if val not in ret:
        ret.append(val)

print ret