from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
a = 0
b = 0
for x in range(5):
    a += x
    if x == 3:
        b = x
assert b == 3