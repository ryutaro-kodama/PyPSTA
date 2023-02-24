import ast
import unittest

from src.rewriter.parent_pointer_rewriter import ParentPointerRewriter
from src.comprehension_converter import ComprehensionConverter
from src.util.util import delete_indent

class TestComprehensionConvert(unittest.TestCase):
    def test_convert1(self):
        code = """
        l = [1, 2, 3]
        x = [y*2 for y in l]
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [1, 2, 3]
        compre1_pypsta = []
        for y_pypsta in l:
            compre1_pypsta.append(y_pypsta * 2)
        x = compre1_pypsta
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

    def test_convert2(self):
        code = """
        l = [1, 2, 3]
        x = [y*2 for y in l if y%2==1]
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [1, 2, 3]
        compre1_pypsta = []
        for y_pypsta in l:
            if y_pypsta % 2 == 1:
                compre1_pypsta.append(y_pypsta * 2)
        x = compre1_pypsta
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

    def test_convert3(self):
        code = """
        l = [1, 2, 3]
        x = [y*2 for y in l if y%2==1 if y%3==1]
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [1, 2, 3]
        compre1_pypsta = []
        for y_pypsta in l:
            if y_pypsta % 2 == 1 and y_pypsta % 3 == 1:
                compre1_pypsta.append(y_pypsta * 2)
        x = compre1_pypsta
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

    def test_convert4(self):
        code = """
        l = [1, 2, 3]
        x = [y*2 if y%2==1 else y for y in l]
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [1, 2, 3]
        compre1_pypsta = []
        for y_pypsta in l:
            if y_pypsta % 2 == 1:
                compre1_pypsta.append(y_pypsta * 2)
            else:
                compre1_pypsta.append(y_pypsta)
        x = compre1_pypsta
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

    def test_convert5(self):
        code = """
        l = [1, 2, 3]
        x = [y*2 if y%2==1 else y for y in l if y%5==1]
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        l = [1, 2, 3]
        compre1_pypsta = []
        for y_pypsta in l:
            if y_pypsta % 5 == 1:
                if y_pypsta % 2 == 1:
                    compre1_pypsta.append(y_pypsta * 2)
                else:
                    compre1_pypsta.append(y_pypsta)
        x = compre1_pypsta
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)

    def test_convert6(self):
        code = """
        ll = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
        x = [y*2 for l in ll for y in l]
        """

        tree = ast.parse(delete_indent(code, 8))
        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
        new_code = ast.unparse(ast.fix_missing_locations(new_tree))

        code2 = """
        ll = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
        compre1_pypsta = []
        for l_pypsta in ll:
            for y_pypsta in l_pypsta:
                compre1_pypsta.append(y_pypsta * 2)
        x = compre1_pypsta
        """
        code2_unparsed = ast.unparse(ast.parse(delete_indent(code2, 8)))

        self.assertEqual(new_code, code2_unparsed)