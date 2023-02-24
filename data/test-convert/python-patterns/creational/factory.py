from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
'*What is this pattern about?\nA Factory is an object for creating other objects.\n\n*What does this example do?\nThe code shows a way to localize words in two languages: English and\nGreek. "get_localizer" is the factory function that constructs a\nlocalizer depending on the language chosen. The localizer object will\nbe an instance from a different class according to the language\nlocalized. However, the main code does not have to worry about which\nlocalizer will be instantiated, since the method "localize" will be called\nin the same way independently of the language.\n\n*Where can the pattern be used practically?\nThe Factory Method can be seen in the popular web framework Django:\nhttps://docs.djangoproject.com/en/4.0/topics/forms/formsets/\nFor example, different types of forms are created using a formset_factory\n\n*References:\nhttp://ginstrom.com/scribbles/2007/10/08/design-patterns-python-style/\n\n*TL;DR\nCreates objects without having to specify the exact class.\n'

class GreekLocalizer:
    """A simple localizer a la gettext"""

    def __init__(self) -> None:
        self.translations = {'dog': 'DOG', 'cat': 'CAT'}

    def localize(self, msg: str) -> str:
        """We'll punt if we don't have a translation"""
        return self.translations.get(msg, msg)

class EnglishLocalizer:
    """Simply echoes the message"""

    def localize(self, msg: str) -> str:
        return msg

def get_localizer(language: str='English') -> object:
    """Factory"""
    localizers = {'English': EnglishLocalizer, 'Greek': GreekLocalizer}
    return localizers[language]()

def main():
    (e, g) = (get_localizer(language='English'), get_localizer(language='Greek'))
    for msg in 'dog parrot cat bear'.split():
        print(e.localize(msg), g.localize(msg))
if __name__ == '__main__':
    main()