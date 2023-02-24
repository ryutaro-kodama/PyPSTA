import ast

from src.rewriter.stared_arg_rewriter import StaredArgRewriter
from src.util.util import delete_indent

class StaredArgConverter:
    """If the argument is passed as stared argument, inform as keyward argument."""
    
    def __init__(self):
        self.stared_arg_rewriter = StaredArgRewriter()

    def convert(self, tree: ast.AST) -> ast.AST:
        """
        If the argument is passed as stared argument, inform as keyward argument.

        Args:
            tree (ast.AST): the AST which you want to convert.
        """
        tree_stared_arg_converted = self.stared_arg_rewriter.visit(tree)
        return tree_stared_arg_converted


if __name__ == "__main__":
    code = """
    def func(a,b,c,d):
        pass
    l = [1,2,3,4]
    func(*l)
    """

    tree = ast.parse(delete_indent(code, 4))
    new_tree = StaredArgConverter().convert(tree)
    new_code = ast.unparse(ast.fix_missing_locations(new_tree))
    print(new_code)