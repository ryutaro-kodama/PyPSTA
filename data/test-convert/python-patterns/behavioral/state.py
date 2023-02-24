from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from __future__ import annotations

class State:
    """Base state. This is to share functionality"""

    def scan(self) -> None:
        """Scan the dial to the next station"""
        self.pos += 1
        if self.pos == len(self.stations):
            self.pos = 0
        print(f'Scanning... Station is {self.stations[self.pos]} {self.name}')

class AmState(State):

    def __init__(self, radio: Radio) -> None:
        self.radio = radio
        self.stations = ['1250', '1380', '1510']
        self.pos = 0
        self.name = 'AM'

    def toggle_amfm(self) -> None:
        print('Switching to FM')
        self.radio.state = self.radio.fmstate

class FmState(State):

    def __init__(self, radio: Radio) -> None:
        self.radio = radio
        self.stations = ['81.3', '89.1', '103.9']
        self.pos = 0
        self.name = 'FM'

    def toggle_amfm(self) -> None:
        print('Switching to AM')
        self.radio.state = self.radio.amstate

class Radio:
    """A radio.     It has a scan button, and an AM/FM toggle switch."""

    def __init__(self) -> None:
        """We have an AM state and an FM state"""
        self.amstate = AmState(self)
        self.fmstate = FmState(self)
        self.state = self.amstate

    def toggle_amfm(self) -> None:
        self.state.toggle_amfm()

    def scan(self) -> None:
        self.state.scan()

def main():
    radio = Radio()
    actions = [radio.scan] * 2 + [radio.toggle_amfm] + [radio.scan] * 2
    actions *= 2
    for action in actions:
        action()
if __name__ == '__main__':
    main()