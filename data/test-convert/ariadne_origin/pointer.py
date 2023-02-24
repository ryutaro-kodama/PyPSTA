from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

def ident(x, y):
    id(x)
    id(y)

class Sample:

    def func(self, x, y):
        print(x)
        print(y)
a = 10
b = 'String'
c = Sample()
i = int(input())
c.func(1.0, 20)
if i == 0:
    a = b
elif i == 1:
    a = c
if i < 0:
    b = c
ident(a, b)