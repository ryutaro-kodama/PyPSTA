import ast
import os

def delete_indent(code: str, indent: int) -> str:
    """
    Delete the indent from each line.
    
    Args:
        code (str): the code you want to delete the indent
        indext (int): the number of spaces which you want to delete
        
    Returns:
        str: the code which indent is deleted
    """
    code_list = code.split('\n')
    new_code_list = [l[indent:] for l in code_list]
    new_code = '\n'.join(new_code_list)
    return new_code

def get_file_name(filepath: str) -> str:
    """
    From file path (absolute or relatively), get file name.

    Args:
        filepath (str): file path including file name at last

    Returns:
        str: file name
    """
    return os.path.split(filepath)[1]

def get_ast(filepath: str) -> ast.AST:
    """
    Get the AST of the code specified by `filepath`.

    Args:
        filepath (str): path to the target file

    Returns:
        ast.AST: the AST of target code
    """
    with open(filepath, 'r', encoding='utf-8') as f:
        code = f.read()
    tree = ast.parse(code)
    return tree

def write_file(filepath: str, contents: str):
    """
    Write or update the file specified by `filepath`.

    Args:
        filepath (str): path to the target file
        contents (str): the contents to be written
    """
    with open(filepath, 'w') as f:
        f.write(contents)
    return