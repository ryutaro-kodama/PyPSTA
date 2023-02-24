import ast

from .insert_rewriter import InsertRewriter, Insert
from .replace_rewriter import ReplaceWriter

class ForElseRewriter(ReplaceWriter):
    """
    Rewrite AST `for-else` node to AST `Name` node and register for-loop expression to `InsertWriter`.
    """

    def __init__(self, insert_rewriter: InsertRewriter):
        """
        Args:
            insert_writer (InsertWriter): 
        """
        super().__init__()
        self.insert_rewriter = insert_rewriter
        return

    def visit_For(self, node: ast.For) -> ast.AST:
        if node.orelse == None or len(node.orelse) == 0:
            # The `for` statement doesn't has `else` statements.
            return super().generic_visit(node)

        # Create iterator createing statements.
        iter_lhs_expr = self._create_temp_var("for_else_iter")
        iter_call_expr = self._create_call_expr(func="pypsta_iter", args=[node.iter])
        iter_assign_expr = self._create_assign_statement(
            targets=[iter_lhs_expr], value=iter_call_expr, lineno=node.lineno)

        # Create getting iterator element statements.
        next_lhs_expr = self._create_temp_var("for_else_target")
        next_call_expr = self._create_call_expr(
            func="pypsta_next", args=[iter_lhs_expr, ast.Constant(value=None)])
        next_assign_expr = self._create_assign_statement(
            targets=[next_lhs_expr], value=next_call_expr, lineno=node.lineno+1)
        
        test = body=ast.Compare(
            left=next_lhs_expr, ops=[ast.IsNot()], comparators=[ast.Constant(value=None)]
        )

        body = node.body
        body.insert(0, ast.Assign(targets=[node.target], value=next_lhs_expr, lineno=node.lineno+3))
        body.append(next_assign_expr)

        # Create `while-else` statement
        new_while = ast.While(test=test, body=body, orelse=node.orelse)

        self.insert_rewriter.register(
            Insert(node.parent, new_while, [iter_assign_expr, next_assign_expr])
        )

        # Replace `for-else` to `while-else`
        return new_while