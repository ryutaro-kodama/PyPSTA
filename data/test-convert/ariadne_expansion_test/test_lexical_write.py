from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
"\nThere is a problem in Ariadne that you can't use builtin functions in comprehension expression.\nThis is because the `ExposedNamesCollector` doesn't work correctly. So in 'pypsta', we use\n`PypstaExposedNameCollector`. This test check whether the problem is solved.\n"

def lexical_use_in_comprehension(l):
    compre1_pypsta = []
    for i_pypsta in range(len(l)):
        compre1_pypsta.append(i_pypsta)
    return compre1_pypsta
a = [10, 20, 30]
b = lexical_use_in_comprehension(a)