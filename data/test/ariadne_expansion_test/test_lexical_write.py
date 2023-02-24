"""
There is a problem in Ariadne that you can't use builtin functions in comprehension expression.
This is because the `ExposedNamesCollector` doesn't work correctly. So in 'pypsta', we use
`PypstaExposedNameCollector`. This test check whether the problem is solved.
"""

def lexical_use_in_comprehension(l):
    return [i for i in range(len(l))]

a = [10,20,30]
b = lexical_use_in_comprehension(a)