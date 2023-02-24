from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
"\n*What is this pattern about?\nThe Borg pattern (also known as the Monostate pattern) is a way to\nimplement singleton behavior, but instead of having only one instance\nof a class, there are multiple instances that share the same state. In\nother words, the focus is on sharing state instead of sharing instance\nidentity.\n\n*What does this example do?\nTo understand the implementation of this pattern in Python, it is\nimportant to know that, in Python, instance attributes are stored in a\nattribute dictionary called __dict__. Usually, each instance will have\nits own dictionary, but the Borg pattern modifies this so that all\ninstances have the same dictionary.\nIn this example, the __shared_state attribute will be the dictionary\nshared between all instances, and this is ensured by assigning\n__shared_state to the __dict__ variable when initializing a new\ninstance (i.e., in the __init__ method). Other attributes are usually\nadded to the instance's attribute dictionary, but, since the attribute\ndictionary itself is shared (which is __shared_state), all other\nattributes will also be shared.\n\n*Where is the pattern used practically?\nSharing state is useful in applications like managing database connections:\nhttps://github.com/onetwopunch/pythonDbTemplate/blob/master/database.py\n\n*References:\n- https://fkromer.github.io/python-pattern-references/design/#singleton\n- https://learning.oreilly.com/library/view/python-cookbook/0596001673/ch05s23.html\n- http://www.aleax.it/5ep.html\n\n*TL;DR\nProvides singleton-like behavior sharing state between instances.\n"
from typing import Dict

class Borg:
    _shared_state: Dict[str, str] = {}

    def __init__(self) -> None:
        self.__dict__ = self._shared_state

class YourBorg(Borg):

    def __init__(self, state: str=None) -> None:
        Borg.__init__(self)
        if state:
            self.state = state
        elif not hasattr(self, 'state'):
            self.state = 'Init'

    def __str__(self) -> str:
        return self.state

def main():
    rm1 = YourBorg()
    rm2 = YourBorg()
    rm1.state = 'Idle'
    rm2.state = 'Running'
    print('rm1: {0}'.format(rm1))
    print('rm2: {0}'.format(rm2))
    rm2.state = 'Zombie'
    print('rm1: {0}'.format(rm1))
    print('rm2: {0}'.format(rm2))
    rm1 is rm2
    rm3 = YourBorg()
    print('rm1: {0}'.format(rm1))
    print('rm2: {0}'.format(rm2))
    print('rm3: {0}'.format(rm3))
    rm4 = YourBorg('Running')
    print('rm4: {0}'.format(rm4))
    print('rm3: {0}'.format(rm3))
if __name__ == '__main__':
    main()