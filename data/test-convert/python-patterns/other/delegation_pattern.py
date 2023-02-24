from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from __future__ import annotations
from typing import Any, Callable

class Delegator:

    def __init__(self, delegate: Delegate) -> None:
        self.delegate = delegate

    def __getattr__(self, name: str) -> Any | Callable:
        attr = getattr(self.delegate, name)
        if not callable(attr):
            return attr

        def wrapper(*args, **kwargs):
            return attr(*args, **kwargs, pypsta_stared_arg1=args)
        return wrapper

class Delegate:

    def __init__(self) -> None:
        self.p1 = 123

    def do_something(self, something: str) -> str:
        return f'Doing {something}'

def main():
    delegator = Delegator(Delegate())
    delegator.p1
    delegator.p2
    delegator.do_something('nothing')
    delegator.do_anything()
if __name__ == '__main__':
    main()