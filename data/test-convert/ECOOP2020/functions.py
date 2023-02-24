from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

def f1(x):
    u = x[0]
    v = x * 3
    return u * v

def f2(x):
    return [x]

def f3(x):
    return x

def f4(x, y):
    u = x['string'] + y
    v = y + 3
    return u | v

def f5(x, y, i):
    t = x * 3
    u = y['string']
    v = x[i]
    return (t[0] + v) * (u + 3)

def f6(x):
    return x['string']

def f7(x):
    if x < 2:
        return 1
    return f7(x - 1) + f7(x - 2)
a = f1([1, 2, 3])
b = f2(a[0])
c = f3(b)
d = f4({'': c[0]}, 1)
e = f5(a, {'': d}, 2)
f = b[e]
g = f6({'st': 1})
h = b[g]