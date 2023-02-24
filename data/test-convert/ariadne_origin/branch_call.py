from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
x = int(input())

def zero(p):
    print('Input 0.')

def non_zero():
    print('Non zero.')

def minus():
    print('minus.')

def plus():
    print('plus.')
if x == 0:
    zero(100)
else:
    non_zero()
    if x < 0:
        minus()
    else:
        plus()

def triple():
    print('3m')

def triple1():
    print('3m + 1')

def triple2():
    print('3m + 2')
    triple()
y = int(input())
if y % 3 == 0:
    triple()
elif y % 3 == 1:
    triple1()
elif y % 3 == 2:
    triple2()
else:
    triple()