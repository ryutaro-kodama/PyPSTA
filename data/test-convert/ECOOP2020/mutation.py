from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

class A:

    def __init__(self):
        self.update(0)

    def update(self, x):
        self.val = x * 2
x = A()
y = x.val
z = x
z.update('a')
if y == 10:
    x.atr = 'b'