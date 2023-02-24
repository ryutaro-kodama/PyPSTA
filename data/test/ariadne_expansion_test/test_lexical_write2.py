"""
There is a problem in Ariadne that if you define variable in container objects, such as tuple,
there is no 'DECL_STMT' in IR. Therefore 'ExposedNamesCollector' can't find the information
and in some situation Ariadne can't produce the 'LexicalWrite' instruction. This problem is solved
by 'PypstaLoader.rewriteAssign2Container'.
"""

TUPLE1, TUPLE2, TUPLE3 = 0, 1, 2
[LIST1, LIST2, LIST3] = "a", "b", "c"

def func1():
    print(TUPLE1)
    print(TUPLE2)
    print(TUPLE3)

def func2():
    print(LIST1)
    print(LIST2)
    print(LIST3)

func1()
func2()