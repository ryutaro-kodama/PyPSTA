from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

def func(d=None):
    if d is None:
        d = {}
    x = d.get('OK')
    print(x)
func()
func({'NG': 1})
func({'OK': 0, 'NG': 1})