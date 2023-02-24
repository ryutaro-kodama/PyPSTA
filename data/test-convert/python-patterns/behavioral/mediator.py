from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from __future__ import annotations

class ChatRoom:
    """Mediator class"""

    def display_message(self, user: User, message: str) -> None:
        print(f'[{user} says]: {message}')

class User:
    """A class whose instances want to interact with each other"""

    def __init__(self, name: str) -> None:
        self.name = name
        self.chat_room = ChatRoom()

    def say(self, message: str) -> None:
        self.chat_room.display_message(self, message)

    def __str__(self) -> str:
        return self.name

def main():
    molly = User('Molly')
    mark = User('Mark')
    ethan = User('Ethan')
    molly.say('Hi Team! Meeting at 3 PM today.')
    mark.say('Roger that!')
    ethan.say('Alright.')
if __name__ == '__main__':
    main()