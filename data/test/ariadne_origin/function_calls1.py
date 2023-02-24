def id(x):
    return x

def call(x, y):
    return x(y)

def foo(a,b):
    return call(id, a+b)

print(foo)
print(foo(1,2))

print(foo(int("10"), 20))

print(call(print, "called!!!"))