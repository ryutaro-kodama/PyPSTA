import ast
import unittest

from src.for_target_converter import ForTargetConverter
from src.util.util import delete_indent

class TestForTargetConverter(unittest.TestCase):
    def test_convert1(self):
        code = """
        l = [(0,"a"),(1,"b"),(2,"c")]
        
        for i,s in l:
            print(i)
            print(s)
        """

        tree = ast.parse(delete_indent(code, 8))
        new_tree = ForTargetConverter().convert(tree)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [(0,"a"),(1,"b"),(2,"c")]
        
        for for_target1_pypsta in l:
            i, s = for_target1_pypsta
            print(i)
            print(s)
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

