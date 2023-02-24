from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from typing import Callable, TypeVar
T = TypeVar('T')

class Dog:

    def __init__(self) -> None:
        self.name = 'Dog'

    def bark(self) -> str:
        return 'woof!'

class Cat:

    def __init__(self) -> None:
        self.name = 'Cat'

    def meow(self) -> str:
        return 'meow!'

class Human:

    def __init__(self) -> None:
        self.name = 'Human'

    def speak(self) -> str:
        return "'hello'"

class Car:

    def __init__(self) -> None:
        self.name = 'Car'

    def make_noise(self, octane_level: int) -> str:
        return f"vroom{'!' * octane_level}"

class Adapter:
    """Adapts an object by replacing methods.

    Usage
    ------
    dog = Dog()
    dog = Adapter(dog, make_noise=dog.bark)
    """

    def __init__(self, obj: T, **adapted_methods: Callable):
        """We set the adapted methods in the object's dict."""
        self.obj = obj
        self.__dict__.update(adapted_methods)

    def __getattr__(self, attr):
        """All non-adapted calls are passed to the object."""
        return getattr(self.obj, attr)

    def original_dict(self):
        """Print original object dict."""
        return self.obj.__dict__

def main():
    objects = []
    dog = Dog()
    print(dog.__dict__)
    objects.append(Adapter(dog, make_noise=dog.bark))
    (objects[0].__dict__['obj'], objects[0].__dict__['make_noise'])
    print(objects[0].original_dict())
    cat = Cat()
    objects.append(Adapter(cat, make_noise=cat.meow))
    human = Human()
    objects.append(Adapter(human, make_noise=human.speak))
    car = Car()
    objects.append(Adapter(car, make_noise=lambda : car.make_noise(3)))
    for obj in objects:
        print('A {0} goes {1}'.format(obj.name, obj.make_noise()))
if __name__ == '__main__':
    main()