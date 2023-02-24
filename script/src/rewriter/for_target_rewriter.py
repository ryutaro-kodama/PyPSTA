import ast

from .replace_rewriter import ReplaceWriter

class ForTargetRewriter(ReplaceWriter):
    """
    Rewrite AST `for-else` node to AST `Name` node and register for-loop expression to `InsertWriter`.
    """

    def __init__(self):
        """
        Args:
            insert_writer (InsertWriter): 
        """
        super().__init__()
        return

    def visit_For(self, node: ast.For) -> ast.AST:
        if isinstance(node.target, ast.Name):
            # The `for` statement whose target is a single variable.
            return super().generic_visit(node)

        # Create new tempolary variable.
        new_target_expr = self._create_temp_var("for_target")

        # Assign the tempolary variable to old target.
        new_target_assign = self._create_assign_statement(
            targets=[node.target], value=new_target_expr, lineno=node.lineno+1
        )

        node.target = new_target_expr
        node.body.insert(0, new_target_assign)

        return node