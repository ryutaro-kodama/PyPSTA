import ast

class ParentPointerRewriter(ast.NodeTransformer):
    """
    Set pointer to parent node in each AST node.
    """
    
    def visit(self, node: ast.AST) -> ast.AST:
        """
        Set pointer to parent node in each AST node.
        """
        for parent in ast.walk(node):
            for child in ast.iter_child_nodes(parent):
                child.parent = parent
        return super().visit(node)