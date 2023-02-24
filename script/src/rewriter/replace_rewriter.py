import ast
from typing import List, Optional, Union

from src.util.constant import Constant


class ReplaceWriter(ast.NodeTransformer):
    def __init__(self) -> None:
        # Use for distinct of identifier
        self.id = 1

    def _create_temp_var(self, prefix: str="") -> ast.Name:
        """
        Create new identifier (AST `Name` node) representing a tempolary variable.

        Args:
            prefix (str): the prefix of the variable.
        
        Returns:
            ast.Name: the new identifier.
        """
        result_identifier = ast.Name(id=f"{prefix}{self.id}_{Constant.PYPSTA}")
        self.id += 1
        return result_identifier

    def _create_call_expr(self,
                          func: Union[str, ast.expr],
                          args: Optional[List[ast.expr]] = None,
                          keywords:Optional[ List[ast.expr]] = None,
                          starargs: Optional[List[ast.expr]] = None,
                          kwargs: Optional[List[ast.expr]] = None
        ) -> ast.Call:
        
        """
        Create call expression (AST `Call` node).

        Args:
            func (Union[str, ast.expr]): the target function's expression or
                function's name.
            args (List[ast.expr], optional): the list of arguments. Defaults to
                None.
            keywords (List[ast.expr], optional): the list of keyword arguments.
                Defaults to None.
            starargs (List[ast.expr], optional): the list of star variadic
                arguments. Defaults to None.
            keywords (List[ast.expr], optional): the list of keyword variadic
                arguments. Defaults to None.

        Returns:
            ast.Call: the call statement
        """
        if isinstance(func, str):
            func=ast.Name(id=func, ctx=ast.Load())
        assert isinstance(func, ast.expr)
        if args is None:
            args = []
        if keywords is None:
            keywords = []
        if starargs is None:
            starargs = []
        if kwargs is None:
            kwargs = []
        return ast.Call(
            func=func, args=args, keywords=keywords, starargs=starargs, kwargs=kwargs
        )

    def _create_assign_statement(self,
                                 targets: List[ast.Name],
                                 value: ast.Expr,
                                 lineno: Optional[int] = None
        ) -> ast.Assign:
        
        """
        Create new assign statement (AST `Assign` node).

        Args:
            targets (List[ast.Name]): the target (left side) expression
            value (ast.Expr): the value (right side) expression
            lineno (Optional[int]): the this assignment's line number

        Returns:
            ast.Assign: the assign statement.
        """
        if lineno is not None:
            return ast.Assign(
                targets=targets, value=value, lineno=lineno, end_lineno=lineno
            )
        else:
            return ast.Assign(targets=targets, value=value)