from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
l = [1, 2, 3, 4, 5]
s = 0
for_else_iter1_pypsta = pypsta_iter(l)
for_else_target2_pypsta = pypsta_next(for_else_iter1_pypsta, None)
while for_else_target2_pypsta is not None:
    i = for_else_target2_pypsta
    s += i
    for_else_target2_pypsta = pypsta_next(for_else_iter1_pypsta, None)
else:
    s = 'Clear'
print(s)