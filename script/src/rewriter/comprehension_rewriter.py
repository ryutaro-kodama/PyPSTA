import ast
from typing import Tuple, Dict

from .insert_rewriter import InsertRewriter, Insert
from .replace_rewriter import ReplaceWriter
from src.util.constant import Constant

class ComprehensionRewriter(ReplaceWriter):
    """
    Rewrite AST `comprehension` node to AST `Name` node and register for-loop expression to `InsertWriter`.
    """

    def __init__(self, insert_writer: InsertRewriter):
        """
        Args:
            insert_writer (InsertWriter): 
        """
        super().__init__()
        self.insert_writer = insert_writer
        return

    def visit_ListComp(self, node: ast.ListComp) -> ast.AST:
        """
        Rewrite AST `ListComp` node to AST `Name` node and register for-loop expression to `InsertWriter`.

        Args:
            node (ast.ListComp): the target `ListComp` expression you want to rewrite

        Returns:
            ast.AST: the rewrited AST node
        """
        return self.__common__(node)

    def visit_GeneratorExp(self, node: ast.GeneratorExp) -> ast.AST:
        """
        Rewrite AST `GeneratorExp` node to AST `Name` node and register for-loop expression to `InsertWriter`.

        Args:
            node (ast.GeneratorExp): the target `GeneratorExp` expression you want to rewrite

        Returns:
            ast.AST: the rewrited AST node
        """
        return self.__common__(node)

    def __common__(self, node) -> ast.AST:
        assign_lineno = node.lineno

        # Make the identifier of result list.
        result_identifier = self._create_temp_var("compre")

        # The difinition statement of result list.
        assign_ = self._create_assign_statement(
            targets=[result_identifier],
            value=ast.List(elts=[]),
            lineno=assign_lineno
        )

        # comprehension_ = ast.increment_lineno(node.generators[0], 1)
        # 'body_expr' means `['body_expr' for x in l]`
        body_expr = ast.increment_lineno(node.elt, 2)

        # Make the expression of the `append` call expression.
        if isinstance(body_expr, ast.IfExp):
            for_def_lineno = assign_lineno + 1
            for_body_start_lineno = for_def_lineno + 1
            for_body_end_lineno = for_body_start_lineno + 3

            # Create 'append' call statement in then statements.
            append_call_in_then = self._create_call_expr(
                func=ast.Attribute(attr="append", value=ast.Name(id=result_identifier.id, ctx=ast.Load())),
                args=[body_expr.body],
            )
            append_call_in_then.lineno = for_body_start_lineno+1
            append_call_in_then.end_lineno = for_body_start_lineno+1

            then_stmt = ast.Expr(
                value=append_call_in_then, lineno=for_body_start_lineno+1, end_lineno=for_body_start_lineno+1
            )

            assert body_expr.orelse is not None
            # Create 'append' call statement in else statements.
            append_call_in_else = self._create_call_expr(
                func=ast.Attribute(attr="append", value=ast.Name(id=result_identifier.id, ctx=ast.Load())),
                args=[body_expr.orelse],
            )
            append_call_in_else.lineno = for_body_start_lineno+3
            append_call_in_else.end_lineno = for_body_start_lineno+3

            else_stmt = ast.Expr(
                value=append_call_in_else, lineno=for_body_start_lineno, end_lineno=for_body_end_lineno
            )

            # Create 'if' statement.
            loop_body_stmt = ast.If(test=body_expr.test, body=[then_stmt], orelse=[else_stmt])
        else:
            for_def_lineno = assign_lineno + 1
            for_body_start_lineno = for_def_lineno + 1
            for_body_end_lineno = for_body_start_lineno

            append_call = self._create_call_expr(
                func=ast.Attribute(attr="append", value=ast.Name(id=result_identifier.id, ctx=ast.Load())),
                args=[body_expr],
            )
            append_call.lineno = for_body_start_lineno
            append_call.end_lineno = for_body_start_lineno

            # Create 'append' call statement.
            loop_body_stmt = ast.Expr(value=append_call, lineno=for_body_start_lineno, end_lineno=for_body_start_lineno)

        # Create for-loop statement.
        for generator in node.generators[::-1]:
            # Add the suffix to target variable.
            self.__rewrite_target_identifier(generator.target, loop_body_stmt, node)

            if len(generator.ifs) > 0:
                test_expr = ast.BoolOp(ast.And(), generator.ifs)

                # For insert 'if' statement, increments line number.
                loop_body_stmt = ast.increment_lineno(loop_body_stmt, 1)

                # Create 'if' statement.
                loop_body_stmt = ast.If(
                    test=test_expr, body=[loop_body_stmt], orelse=[],
                    lino=for_body_start_lineno, end_lino=for_body_start_lineno+1
                )

                for_body_end_lineno += 1
                loop_body_stmt = self.__create_for_loop(
                    target=generator.target, iter=generator.iter, body_stmt=loop_body_stmt,
                    lineno=for_def_lineno, end_lineno=for_body_end_lineno
                )
            else:
                loop_body_stmt = self.__create_for_loop(
                    target=generator.target, iter=generator.iter, body_stmt=loop_body_stmt,
                    lineno=for_def_lineno, end_lineno=for_body_end_lineno
                )
        for_stmt = loop_body_stmt

        parent = node.parent
        grand_parent = parent.parent
        while not hasattr(grand_parent, "body"):
            parent = grand_parent
            grand_parent = grand_parent.parent
            assert grand_parent is not None

        # Register to insert rewriter to insert assign statement and for-loop statement.
        self.insert_writer.register(
            Insert(grand_parent, parent, [assign_, for_stmt])
        )

        return ast.Name(id=result_identifier.id, lineno=node.lineno, end_lineno=node.end_lineno)

    def __create_for_loop(self, target: ast.Expr, iter: ast.Expr, body_stmt: ast.stmt, lineno: int, end_lineno: int) -> ast.For:
        """
        Create new for-loop statement (AST `For` node) to represent the effect of comprehension.

        Args:
            target (ast.Expr): the target expression (`for <target> in iter:`)
            iter (ast.Expr): the iter expression (`for target in <iter>:`)
            body_expr (ast.stmt): the body statement executed in for-loop
            lineno (int): the start line number of this for-loop statement
            end_lineno (int): the end line number of this for-loop statement

        Returns:
            ast.For: new for-loop statement to represent the effect of comprehension.
        """
        return ast.For(
            target=target, iter=iter, body=[body_stmt], orelse=[], lineno=lineno, end_lineno=end_lineno
        )

    def __rewrite_target_identifier(self, target_expr: ast.Expr, *obey_exprs: Tuple[ast.Expr]) -> None:
        """
        Rewrite the identifier. In node under `target_expr`, convert old identifier to new identifier and register
        relation of conversion. In nodes under `obey_expr`, according to the relation, convert identifier.

        Args:
            target_expr (ast.Expr): the root expression in which you create new relation of convert
            obey_exprs (List[ast.Expr]): the list of root expression in which you convert according to relation
        """
        identifier_suffix_rewriter = IdentifierSuffixRewriter()
        identifier_suffix_rewriter.visit(target_expr)
        identifier_convert_rewriter = IdentifierConvertRewriter(identifier_suffix_rewriter.rewrite_id_table)
        for obey_expr in obey_exprs:
            identifier_convert_rewriter.visit(obey_expr)
        return

    def visit_SetComp(self, node: ast.SetComp) -> ast.AST:
        assert False
        return super().visit_SetComp(node)

    def visit_DictComp(self, node: ast.DictComp) -> ast.AST:
        assert False
        return super().visit_DictComp(node)

class IdentifierSuffixRewriter(ast.NodeTransformer):
    """
    Append suffix to the identifier. 
    """

    SUFFIX = "_" + Constant.PYPSTA

    def __init__(self):
        self.rewrite_id_table = {}
        return

    def visit_Name(self, node: ast.Name) -> ast.AST:
        """
        Rewrite the ast Name node, which the suffix is appended to the identifier.

        Args:

        """
        new_id = node.id + IdentifierSuffixRewriter.SUFFIX
        self.rewrite_id_table[node.id] = new_id
        node.id = new_id
        return node

class IdentifierConvertRewriter(ast.NodeTransformer):
    """
    Convert the identifier according to table.
    """

    def __init__(self, rewrite_id_table: Dict[str, str]):
        """
        Args:
            rewrite_id_table (Dict[str, str]): the table of which the key is old identifier
                and the value is new identifier
        """
        self.rewrite_id_table = rewrite_id_table
        return

    def visit_Name(self, node: ast.Name) -> ast.AST:
        """
        Rewrite the identifier
        """
        if node.id in self.rewrite_id_table:
            new_id = self.rewrite_id_table[node.id]
            node.id = new_id
        return node
            