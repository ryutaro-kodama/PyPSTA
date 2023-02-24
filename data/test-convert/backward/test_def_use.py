from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
i = int(input())
if i == -1:
    b = print
else:
    b = input
if i == 0:
    a = 10 * i
else:
    a = 20 * i
b()