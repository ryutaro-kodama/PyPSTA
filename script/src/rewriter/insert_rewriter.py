import ast
from typing import List, Optional, Set, Tuple

class Insert:
    """
    This is the data class, which saves the insert index and insert node.
    """

    def __init__(self, target_node: ast.AST, insert_point_node: ast.AST, inserted_node: List[ast.AST]) -> None:
        """Hold the ast nodes which you want to insert to.

        Args:
            target_node (ast.AST): the AST node to which you insert new node.
            insert_point_node (ast.AST): the AST node of which you insert immediatly before.
            inserted_node (ast.AST): the ast nodes which you want to insert to.
        """
        self.target_node = target_node
        self.insert_point_node = insert_point_node
        self.inserted_node = inserted_node
        return

    def is_modify_node(self, node: ast.AST) -> bool:
        """
        Return whether the argument node is this target node.

        Args:
            node (ast.AST): the AST node which we want to check

        Returns:
            bool: true if the argument node equals this target node
        """
        return node == self.target_node

    def zip(self) -> Tuple[int, ast.AST]:
        """
        Return the tuple of which 1st element is the index of insert point and 2nd element is the AST node.

        Returns:
            Tuple[int, ast.AST]: the the tuple of which 1st element is the index of insert point
                and 2nd element is the AST node
        """
        for index2inserted_node in self.index2inserted_node_list:
            yield index2inserted_node
            

class InsertRewriter(ast.NodeTransformer):
    """
    Rewrite (insert) the new node to target list.
    """

    def __init__(self):
        self.insert_set: Set[Insert] = set()
        return

    def register(self, insert: Insert) -> None:
        """
        Register the `Insert` object.

        Args:
            insert (Insert): the `Insert` object which you want to do
        """
        self.insert_set.add(insert)
        return

    def get_and_delete_insert(self, node: ast.AST) -> Optional[Insert]:
        """
        Get the `Insert` object whose target node is `node`. If there is delete it from set and return it.

        Args:
            node (ast.AST): the AST node which you want to check whether this is target node or not

        Returns:
            Optional[Insert]: the `Insert` object whose target node is `node`, otherwise return `None`
        """
        for insert in self.insert_set:
            if insert.is_modify_node(node):
                self.insert_set.remove(insert)
                return insert
        else:
            return None

    def visit(self, node: ast.AST) -> ast.AST:
        """
        Visit all nodes and check whether this node is target node. If so, get `Insert` object 
        and insert the node to this node's list element

        Args:
            node (ast.AST): the AST node 
        """
        while (insert := self.get_and_delete_insert(node)) != None:
            if isinstance(node, ast.If) and insert.insert_point_node in node.orelse:
                insert_index = node.orelse.index(insert.insert_point_node)
                target_list = node.orelse
            else:
                insert_index = node.body.index(insert.insert_point_node)
                target_list = node.body

            for new_node in insert.inserted_node:
                target_list.insert(insert_index, new_node)
                for after_node in target_list[insert_index+1:]:
                    # Increment line number after this node.
                    ast.increment_lineno(after_node, 1)
                insert_index += 1

        return super().visit(node)