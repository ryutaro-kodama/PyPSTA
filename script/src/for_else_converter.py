import ast

from src.rewriter.insert_rewriter import InsertRewriter
from src.rewriter.for_else_rewriter import ForElseRewriter
from src.rewriter.parent_pointer_rewriter import ParentPointerRewriter
from src.util.util import delete_indent

class ForElseConverter:
    """
    Convert `for-else` statement to `while-else` statement.
    """
    
    def __init__(self):
        self.insert_rewriter = InsertRewriter()
        self.for_else_rewriter = ForElseRewriter(self.insert_rewriter)

    def convert(self, tree: ast.AST) -> ast.AST:
        """
        Convert `for-else` statement to `while-else` statement.

        Args:
            tree (ast.AST): the AST which you want to convert and each node has the pointer to parent
        """
        tree_for_else_converted = self.for_else_rewriter.visit(tree)
        tree_iter_inserted = self.insert_rewriter.visit(tree_for_else_converted)
        return tree_iter_inserted


if __name__ == "__main__":
    code = """
    l = [1,2,3]
    
    for i in l:
        print(i)
    else:
        print("finish")
    """

    tree = ast.parse(delete_indent(code, 4))
    tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
    new_tree = ForElseConverter().convert(tree_has_parent_pointer)
    new_code = ast.unparse(ast.fix_missing_locations(new_tree))
    print(new_code)

    code2 = """
    l = [1,2,3]

    for_else_iter1_pypsta = iter(l)
    for_else_target2_pypsta = next(for_else_iter1_pypsta, None)
    while for_else_target2_pypsta is not None:
        i = for_else_target2_pypsta
        print(i)
        for_else_target2_pypsta = next(for_else_iter1_pypsta, None)
    else:
        print("finish")
    """
    # tree2 = ast.parse(delete_indent(code2, 4))
    # print(ast.dump(tree2, indent=4))