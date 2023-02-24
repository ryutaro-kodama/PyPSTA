def func1(a, b, c, d='d_def', *e, **f):
    print(a)
    print(b)
    print(c)
    print(d)
    print(e)
    print(f)
    print('----------------')
func1(1, 2, 3, 4, 5, 6, 7)
func1(10, 20, 30, 40, 50, 60, 70, x=80)
func1(1000, 2000, c=3000)
func1(10000, 20000, 30000, 40000, e='50000')

def func2(w=False, x=999, y="YYY", z=9.99):
    print(w)
    print(x)
    print(y)
    print(z)
    print('----------------')
func2(1, 2, 3, 4)
func2(10, 20)
func2(y=100, z=200, x=400, w=300)

class Sample:
    def __init__(self, defarg = 10):
        print(defarg)

    def func(self, defarg = "a"):
        print(defarg)

sample1 = Sample()
sample2 = Sample(20)

sample1.func()
sample1.func("b")