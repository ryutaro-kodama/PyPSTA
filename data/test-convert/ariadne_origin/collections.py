from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

def id(x):
    return x

def listTest():
    l = [1, 2, 3, 4]
    id(l[2])
    l[2] = 5
    id(l)

def tupleTest():
    t = (11, 12, 13, 14)
    id(t[2])
    id(t)

def setTest():
    s = {21, 22, 23, 24}
    id(s)

def dictTest():
    d = {31: 41, 32: '42', 'hey': 'hi', '33': 43}
    id(d)
    id(d['hey'])
    d['hey'] = 'hey'
    var = d['hey']
    id(var)
listTest()
tupleTest()
setTest()
dictTest()