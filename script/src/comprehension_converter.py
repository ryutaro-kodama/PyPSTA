import ast

from src.rewriter.insert_rewriter import InsertRewriter
from src.rewriter.comprehension_rewriter import ComprehensionRewriter
from src.rewriter.parent_pointer_rewriter import ParentPointerRewriter
from src.util.util import delete_indent

class ComprehensionConverter:
    """
    Convert comprehension expression to for-loop expression
    """
    
    def __init__(self):
        self.insert_rewriter = InsertRewriter()
        self.comprehension_rewriter = ComprehensionRewriter(self.insert_rewriter)

    def convert(self, tree: ast.AST) -> ast.AST:
        """
        Convert comprehension expression to for-loop expression.

        Args:
            tree (ast.AST): the AST which you want to convert and each node has the pointer to parent
        """
        tree_comprehension_deleted = self.comprehension_rewriter.visit(tree)
        tree_forloop_inserted = self.insert_rewriter.visit(tree_comprehension_deleted)
        return tree_forloop_inserted


if __name__ == "__main__":
    code = """
    l = [1,2,3]
    x = [y*2 for y in l]
    """

    tree = ast.parse(delete_indent(code, 4))
    tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
    new_tree = ComprehensionConverter().convert(tree_has_parent_pointer)
    new_code = ast.unparse(ast.fix_missing_locations(new_tree))
    print(new_code)

    code2 = """
    l = [1,2,3]
    compre1_pypsta = []
    for y in l:
        compre1_pypsta.append(y * 2)
    x = compre1_pypsta
    """