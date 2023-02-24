import ast

from src.rewriter.for_target_rewriter import ForTargetRewriter
from src.rewriter.parent_pointer_rewriter import ParentPointerRewriter
from src.util.util import delete_indent

class ForTargetConverter:
    """
    If the target expression of `for` statement is not single variable, convert
    to single variable.
    """
    
    def __init__(self):
        self.for_taerget_rewriter = ForTargetRewriter()

    def convert(self, tree: ast.AST) -> ast.AST:
        """
        If the target expression of `for` statement is not single variable, convert
        to single variable.

        Args:
            tree (ast.AST): the AST which you want to convert.
        """
        tree_for_target_converted = self.for_taerget_rewriter.visit(tree)
        return tree_for_target_converted


if __name__ == "__main__":
    code = """
    l = [(0,"a"),(1,"b"),(2,"c")]
    
    for i,s in l:
        print(i)
        print(s)
    """

    tree = ast.parse(delete_indent(code, 4))
    new_tree = ForTargetConverter().convert(tree)
    new_code = ast.unparse(ast.fix_missing_locations(new_tree))
    print(new_code)

    code2 = """
    l = [(0,"a"),(1,"b"),(2,"c")]
    
    for for_target1_pypsta in l:
        i, s = for_target1_pypsta
        print(i)
        print(s)
    """
    # tree2 = ast.parse(delete_indent(code2, 4))
    # print(ast.dump(tree2, indent=4))