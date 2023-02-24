import os
import unittest

from main import get_write_filepath

class TestMain(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # I assume that current directory is 'pypsta/script/test' but there is
        # some case that current directory is not it. So consider reflect
        # the difference to arguments.
        cwd = os.getcwd()
        assume_cwd = os.path.split(__file__)[0]
        cls.sub = os.path.relpath(assume_cwd, start=cwd)

        global get_write_filepath
        origin = get_write_filepath

        def wrapper(*arg):
            arg0 = os.path.join(cls.sub, arg[0])
            arg1 = os.path.join(cls.sub, arg[1])
            arg2 = os.path.abspath(os.path.join(cls.sub, arg[2]))
            return origin(arg0, arg1, arg2)
        
        get_write_filepath = wrapper

    def test_get_write_filepath1(self):
        """When specify the file as input."""
        self.assertEqual(
            get_write_filepath(
                ".\\data\\dir\\in\\file.py", ".\\data\\dir\\in\\file.py",
                "data\\dir\\out\\"
            ),
            self.__make_expected("data\\dir\\out\\file.py")
        )

    def test_get_write_filepath2(self):
        """
        When specify the directory which has a file directly beneath as input.
        """
        self.assertEqual(
            get_write_filepath(
                ".\\data\\dir\\in\\file.py", ".\\data\\dir\\in\\",
                "data\\dir\\out\\"
            ),
           self.__make_expected("data\\dir\\out\\file.py")
        )

    def test_get_write_filepath3(self):
        """
        When specify the directory which has a file directly beneath as input
        and the last of specification doesn't has slash
        """
        self.assertEqual(
            get_write_filepath(
                ".\\data\\dir\\in\\file.py", ".\\data\\dir\\in",
                "data\\dir\\out\\"
            ),
            self.__make_expected("data\\dir\\out\\file.py")
        )

    def test_get_write_filepath4(self):
        """
        When specify the directory which has a file indirectly beneath as input.
        """
        self.assertEqual(
            get_write_filepath(
                ".\\data\\dir\\in\\a\\file2.py", ".\\data\\dir\\in\\",
                "data\\dir\\out\\"
            ),
            self.__make_expected("data\\dir\\out\\a\\file2.py")
        )

    def test_get_write_filepath5(self):
        """
        When specify the directory which has a file indirectly beneath as input
        and the last of specification doesn't has slash
        """
        self.assertEqual(
            get_write_filepath(
                ".\\data\\dir\\in\\a\\file2.py", ".\\data\\dir\\in",
                "data\\dir\\out\\"
            ),
           self.__make_expected("data\\dir\\out\\a\\file2.py")
        )

    def __make_expected(self, path):
        return os.path.abspath(os.path.join(self.sub, path))
