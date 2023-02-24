import ast

from src.rewriter.add_import_rewriter import AddImportRewriter
from src.util.util import delete_indent

class MockImporter:
    def __init__(self):
        self.add_import_rewriter = AddImportRewriter(
            "from pypsta_mock import range",
            "from pypsta_mock import pypsta_iter",
            "from pypsta_mock import pypsta_next",
            "from pypsta_mock import pypsta_slice",
        )
        return

    def convert(self, tree: ast.AST) -> ast.AST:
        """
        Convert AST to that mock import statement is inserted.

        Args:
            tree (ast.AST): target AST

        Returns:
            ast.AST: the new AST
        """
        new_tree = self.add_import_rewriter.visit(tree)
        return new_tree


if __name__ == "__main__":
    code = """
    x = 10
    y = 20
    z = x + y
    """

    tree = ast.parse(delete_indent(code, 4))
    new_tree = MockImporter().convert(tree)
    new_code = ast.unparse(ast.fix_missing_locations(new_tree))
    print(new_code)