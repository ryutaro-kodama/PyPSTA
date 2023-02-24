from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
"\n*What is this pattern about?\nIt decouples the creation of a complex object and its representation,\nso that the same process can be reused to build objects from the same\nfamily.\nThis is useful when you must separate the specification of an object\nfrom its actual representation (generally for abstraction).\n\n*What does this example do?\n\nThe first example achieves this by using an abstract base\nclass for a building, where the initializer (__init__ method) specifies the\nsteps needed, and the concrete subclasses implement these steps.\n\nIn other programming languages, a more complex arrangement is sometimes\nnecessary. In particular, you cannot have polymorphic behaviour in a constructor in C++ -\nsee https://stackoverflow.com/questions/1453131/how-can-i-get-polymorphic-behavior-in-a-c-constructor\n- which means this Python technique will not work. The polymorphism\nrequired has to be provided by an external, already constructed\ninstance of a different class.\n\nIn general, in Python this won't be necessary, but a second example showing\nthis kind of arrangement is also included.\n\n*Where is the pattern used practically?\n\n*References:\nhttps://sourcemaking.com/design_patterns/builder\n\n*TL;DR\nDecouples the creation of a complex object and its representation.\n"

class Building:

    def __init__(self) -> None:
        self.build_floor()
        self.build_size()

    def build_floor(self):
        raise NotImplementedError

    def build_size(self):
        raise NotImplementedError

    def __repr__(self) -> str:
        return 'Floor: {0.floor} | Size: {0.size}'.format(self)

class House(Building):

    def __init__(self) -> None:
        Building.__init__(self)

    def build_floor(self) -> None:
        self.floor = 'One'

    def build_size(self) -> None:
        self.size = 'Big'

class Flat(Building):

    def __init__(self) -> None:
        Building.__init__(self)

    def build_floor(self) -> None:
        self.floor = 'More than One'

    def build_size(self) -> None:
        self.size = 'Small'

class ComplexBuilding:

    def __repr__(self) -> str:
        return 'Floor: {0.floor} | Size: {0.size}'.format(self)

class ComplexHouse(ComplexBuilding):

    def build_floor(self) -> None:
        self.floor = 'One'

    def build_size(self) -> None:
        self.size = 'Big and fancy'

def construct_building(cls) -> Building:
    building = cls()
    building.build_floor()
    building.build_size()
    return building

def main():
    house = House()
    flat = Flat()
    complex_house = construct_building(ComplexHouse)
    complex_house
if __name__ == '__main__':
    main()