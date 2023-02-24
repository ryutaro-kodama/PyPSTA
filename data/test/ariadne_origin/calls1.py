base_init = 10

def id(x):
    return x
    
def func(x):
    return x

def call(x, y):
    return x(y)

def foo(a,b):
    return call(id, a+b)

class Foo():
    base = base_init

    def foo(self, a, b):
        self.contents = id(a+b+self.base)
        return self.contents
int("100")

func(Foo)

func(Foo.foo)
func(Foo.base)

func(foo)
func(foo(1,2))

instance = Foo()
func(Foo.foo(instance, 2,3))
func(instance.foo(3,4))

f = instance.foo
func(f)
func(f(4,5))

instance.f = foo
func(instance.f(5,6))
func(instance.f)

instance.foo = foo
func(instance.foo(6,7))
func(instance.foo)

foo.x = foo
func(foo.x(7,8))
func(foo.x)

x = Foo
func(x)
y = x()
func(y)
func(y.foo(8,9))

def nothing():
    return 0

z = id(nothing)
z()