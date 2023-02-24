from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

class Sample:

    def __call__(self, arg):
        print(arg)
s = Sample()
s(111)