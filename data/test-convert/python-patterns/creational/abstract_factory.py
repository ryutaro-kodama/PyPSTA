from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
'\n*What is this pattern about?\n\nIn Java and other languages, the Abstract Factory Pattern serves to provide an interface for\ncreating related/dependent objects without need to specify their\nactual class.\n\nThe idea is to abstract the creation of objects depending on business\nlogic, platform choice, etc.\n\nIn Python, the interface we use is simply a callable, which is "builtin" interface\nin Python, and in normal circumstances we can simply use the class itself as\nthat callable, because classes are first class objects in Python.\n\n*What does this example do?\nThis particular implementation abstracts the creation of a pet and\ndoes so depending on the factory we chose (Dog or Cat, or random_animal)\nThis works because both Dog/Cat and random_animal respect a common\ninterface (callable for creation and .speak()).\nNow my application can create pets abstractly and decide later,\nbased on my own criteria, dogs over cats.\n\n*Where is the pattern used practically?\n\n*References:\nhttps://sourcemaking.com/design_patterns/abstract_factory\nhttp://ginstrom.com/scribbles/2007/10/08/design-patterns-python-style/\n\n*TL;DR\nProvides a way to encapsulate a group of individual factories.\n'
import random
from typing import Type

class Pet:

    def __init__(self, name: str) -> None:
        self.name = name

    def speak(self) -> None:
        raise NotImplementedError

    def __str__(self) -> str:
        raise NotImplementedError

class Dog(Pet):
    def __init__(self, name: str) -> None:
        Pet.__init__(self, name)

    def speak(self) -> None:
        print('woof')

    def __str__(self) -> str:
        return f'Dog<{self.name}>'

class Cat(Pet):
    def __init__(self, name: str) -> None:
        Pet.__init__(self, name)

    def speak(self) -> None:
        print('meow')

    def __str__(self) -> str:
        return f'Cat<{self.name}>'

class PetShop:
    """A pet shop"""

    def __init__(self, animal_factory: Type[Pet]) -> None:
        """pet_factory is our abstract factory.  We can set it at will."""
        self.pet_factory = animal_factory

    def buy_pet(self, name: str) -> Pet:
        """Creates and shows a pet using the abstract factory"""
        pet = self.pet_factory(name)
        print(f'Here is your lovely {pet}')
        return pet

def random_animal(name: str) -> Pet:
    """Let's be dynamic!"""
    return random.choice([Dog, Cat])(name)

def main() -> None:
    cat_shop = PetShop(Cat)
    pet = cat_shop.buy_pet('Lucy')
    pet.speak()
    shop = PetShop(random_animal)
    for name in ['Max', 'Jack', 'Buddy']:
        pet = shop.buy_pet(name)
        pet.speak()
        print('=' * 20)
if __name__ == '__main__':
    main()