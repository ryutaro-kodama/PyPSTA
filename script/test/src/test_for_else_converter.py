import ast
import unittest

from src.for_else_converter import ForElseConverter
from src.rewriter.parent_pointer_rewriter import ParentPointerRewriter
from src.util.util import delete_indent

class TestForElseConverter(unittest.TestCase):
    def test_convert1(self):
        code = """
        l = [1,2,3]
        
        for i in l:
            print(i)
        else:
            print("finish")
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ForElseConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [1,2,3]

        for_else_iter1_pypsta = pypsta_iter(l)
        for_else_target2_pypsta = pypsta_next(for_else_iter1_pypsta, None)
        while for_else_target2_pypsta is not None:
            i = for_else_target2_pypsta
            print(i)
            for_else_target2_pypsta = pypsta_next(for_else_iter1_pypsta, None)
        else:
            print("finish")
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

