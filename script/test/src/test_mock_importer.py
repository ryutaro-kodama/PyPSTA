import ast
import unittest

from src.mock_importer import MockImporter
from src.util.util import delete_indent

class TestMockImporter(unittest.TestCase):
    def test_convert1(self):
        code = """
        x = 10
        y = 20
        z = x + y
        """

        tree = ast.parse(delete_indent(code, 8))
        new_tree = MockImporter().convert(tree)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        from pypsta_mock import range
        from pypsta_mock import pypsta_iter
        from pypsta_mock import pypsta_next
        from pypsta_mock import pypsta_slice
        x = 10
        y = 20
        z = x + y
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

