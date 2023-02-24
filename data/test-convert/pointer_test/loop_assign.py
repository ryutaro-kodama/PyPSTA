from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

class Point(object):

    def func(self, data):
        self.data = data

    def output(self):
        print(self.data)
n = 5
l = [0, 0, 0, 0, 0]
for i in range(n):
    l[i] = Point()
for p in l:
    p.func(10)
    p.output()
x = int()
print(x)
print(l[x])