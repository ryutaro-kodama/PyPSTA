import ast
from typing import List, Union

class AddImportRewriter(ast.NodeTransformer):
    def __init__(self, *import_str_list: List[str]):
        """
        Args:
            import_str_list (List[str]): the list of import statement
                which you want to insert to file
        """
        self.visited = False
        self.import_str_list = import_str_list

    def visit_Module(self, node: ast.Module) -> ast.AST:
        """
        In the `ast.Module` node, parse the import string code to AST,
        and insert it to the first element.

        Args:
            node (ast.Module): the AST node which you will insert import node

        Returns:
            ast.AST: the new AST node
        """
        # Assume that we visit `ast.Module` only once.
        assert not self.visited
        self.visited = True

        index = 0
        for import_str in self.import_str_list:
            # Get import AST node.
            import_ = self.__parse(import_str)

            # Insert to first element.
            node.body.insert(index, import_)
            index += 1

            for after_node in node.body[index+1:]:
                # Increment line number after inserted node.
                ast.increment_lineno(after_node, 1)

        return node

    def __parse(self, import_str) -> Union[ast.Import, ast.ImportFrom]:
        """
        Parse import string code to AST.

        Args:
            import_str (str): the import string code

        Returns:
            Union[ast.Import, ast.ImportFrom]: the import AST
        """
        tree = ast.parse(import_str)
        assert isinstance(tree, ast.Module)

        import_ = tree.body[0]
        return import_