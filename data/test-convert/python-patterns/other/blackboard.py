from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from __future__ import annotations
from abc import ABCMeta, abstractmethod
import random

class Blackboard:

    def __init__(self) -> None:
        self.experts = []
        self.common_state = {'problems': 0, 'suggestions': 0, 'contributions': [], 'progress': 0}

    def add_expert(self, expert: AbstractExpert) -> None:
        self.experts.append(expert)

class Controller:

    def __init__(self, blackboard: Blackboard) -> None:
        self.blackboard = blackboard

    def run_loop(self):
        """
        This function is a loop that runs until the progress reaches 100.
        It checks if an expert is eager to contribute and then calls its contribute method.
        """
        while self.blackboard.common_state['progress'] < 100:
            for expert in self.blackboard.experts:
                if expert.is_eager_to_contribute:
                    expert.contribute()
        return self.blackboard.common_state['contributions']

class AbstractExpert(metaclass=ABCMeta):

    def __init__(self, blackboard: Blackboard) -> None:
        self.blackboard = blackboard

    @property
    @abstractmethod
    def is_eager_to_contribute(self):
        raise NotImplementedError('Must provide implementation in subclass.')

    @abstractmethod
    def contribute(self):
        raise NotImplementedError('Must provide implementation in subclass.')

class Student(AbstractExpert):

    def __init__(self, blackboard: Blackboard) -> None:
        AbstractExpert.__init__(self, blackboard)

    @property
    def is_eager_to_contribute(self) -> bool:
        return True

    def contribute(self) -> None:
        self.blackboard.common_state['problems'] += random.randint(1, 10)
        self.blackboard.common_state['suggestions'] += random.randint(1, 10)
        self.blackboard.common_state['contributions'] += [self.__class__.__name__]
        self.blackboard.common_state['progress'] += random.randint(1, 2)

class Scientist(AbstractExpert):

    def __init__(self, blackboard: Blackboard) -> None:
        AbstractExpert.__init__(self, blackboard)

    @property
    def is_eager_to_contribute(self) -> int:
        return random.randint(0, 1)

    def contribute(self) -> None:
        self.blackboard.common_state['problems'] += random.randint(10, 20)
        self.blackboard.common_state['suggestions'] += random.randint(10, 20)
        self.blackboard.common_state['contributions'] += [self.__class__.__name__]
        self.blackboard.common_state['progress'] += random.randint(10, 30)

class Professor(AbstractExpert):

    def __init__(self, blackboard: Blackboard) -> None:
        AbstractExpert.__init__(self, blackboard)

    @property
    def is_eager_to_contribute(self) -> bool:
        return True if self.blackboard.common_state['problems'] > 100 else False

    def contribute(self) -> None:
        self.blackboard.common_state['problems'] += random.randint(1, 2)
        self.blackboard.common_state['suggestions'] += random.randint(10, 20)
        self.blackboard.common_state['contributions'] += [self.__class__.__name__]
        self.blackboard.common_state['progress'] += random.randint(10, 100)

def main():
    blackboard = Blackboard()
    blackboard.add_expert(Student(blackboard))
    blackboard.add_expert(Scientist(blackboard))
    blackboard.add_expert(Professor(blackboard))
    c = Controller(blackboard)
    contributions = c.run_loop()
if __name__ == '__main__':
    random.seed(1234)
    main()