from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
"\n*What is this pattern about?\nThis patterns aims to reduce the number of classes required by an\napplication. Instead of relying on subclasses it creates objects by\ncopying a prototypical instance at run-time.\n\nThis is useful as it makes it easier to derive new kinds of objects,\nwhen instances of the class have only a few different combinations of\nstate, and when instantiation is expensive.\n\n*What does this example do?\nWhen the number of prototypes in an application can vary, it can be\nuseful to keep a Dispatcher (aka, Registry or Manager). This allows\nclients to query the Dispatcher for a prototype before cloning a new\ninstance.\n\nBelow provides an example of such Dispatcher, which contains three\ncopies of the prototype: 'default', 'objecta' and 'objectb'.\n\n*TL;DR\nCreates new object instances by cloning prototype.\n"
from __future__ import annotations
from typing import Any

class Prototype:

    def __init__(self, value: str='default', **attrs: Any) -> None:
        self.value = value
        self.__dict__.update(attrs)

    def clone(self, **attrs: Any) -> Prototype:
        """Clone a prototype and update inner attributes dictionary"""
        obj = self.__class__(**self.__dict__)
        obj.__dict__.update(attrs)
        return obj

class PrototypeDispatcher:

    def __init__(self):
        self._objects = {}

    def get_objects(self) -> dict[str, Prototype]:
        """Get all objects"""
        return self._objects

    def register_object(self, name: str, obj: Prototype) -> None:
        """Register an object"""
        self._objects[name] = obj

    def unregister_object(self, name: str) -> None:
        """Unregister an object"""
        del self._objects[name]

def main() -> None:
    dispatcher = PrototypeDispatcher()
    prototype = Prototype()
    d = prototype.clone()
    a = prototype.clone(value='a-value', category='a')
    b = a.clone(value='b-value', is_checked=True)
    dispatcher.register_object('objecta', a)
    dispatcher.register_object('objectb', b)
    dispatcher.register_object('default', d)
    compre1_pypsta = []
    for for_target1_pypsta in dispatcher.get_objects().items():
        (n_pypsta, p_pypsta) = for_target1_pypsta
        compre1_pypsta.append({n_pypsta: p_pypsta.value})
    compre1_pypsta
    print(b.category, b.is_checked)
if __name__ == '__main__':
    main()