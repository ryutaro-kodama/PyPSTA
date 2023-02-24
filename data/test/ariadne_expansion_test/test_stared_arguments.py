def func1(a, b, c, d):
    print(a)
    print(b)
    print(c)
    print(d)
    print(e)
    print(f)
    print('----------------')
l = [1,2,3,4]
func1(*l)

def func2(w, x, y, z="a"):
    print(w)
    print(x)
    print(y)
    print(z)
    print('----------------')
t = (10, 20, 30)
func2(*t)
t2 = (100, 200, 300, 400)
func2(*t2)