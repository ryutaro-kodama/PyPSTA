import ast

from .replace_rewriter import ReplaceWriter

class StaredArgRewriter(ReplaceWriter):
    """
    Rewrite stared AST `call` node's keywords.
    """
    
    def __init__(self) -> None:
        super().__init__()
        self.id = 1

    def visit_Call(self, node: ast.Call) -> ast.AST:
        for arg in node.args:
            if isinstance(arg, ast.Starred):
                assert isinstance(arg.value, ast.Name)
                lineno = arg.lineno

                var_name = arg.value.id
                val_target = ast.Name(id=var_name, lineno=lineno, end_lineno=lineno)

                keyword_name = f'pypsta_stared_arg{self.id}'
                self.id += 1

                keyword = ast.keyword(
                    arg=keyword_name, value=val_target, lineno=lineno, end_lineno=lineno)
                node.keywords.append(keyword)
        return node