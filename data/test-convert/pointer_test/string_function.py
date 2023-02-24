from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

def sample():
    return 'OK'
l = [None]
a = sample()
print(a)
l[0] = a