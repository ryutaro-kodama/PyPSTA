import ast
import argparse
from logging import basicConfig, getLogger, DEBUG, INFO
import glob
import os
import unittest

from src.rewriter.parent_pointer_rewriter import ParentPointerRewriter
from src.comprehension_converter import ComprehensionConverter
from src.for_target_converter import ForTargetConverter
from src.for_else_converter import ForElseConverter
from src.mock_importer import MockImporter
from src.stared_arg_converter import StaredArgConverter
from src.util.util import get_file_name, get_ast, write_file


basicConfig(level=DEBUG)
logger = getLogger(__name__)

def set_arg_parser():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(title="subcommands", description="valid subcommands", help="select the mode what you want to from below")

    parser_convert = subparsers.add_parser("convert", help="convert input files")
    parser_convert.add_argument("filepath", help="the file to be converted")
    parser_convert.add_argument("-o", "--output_directory", default=".", help="the directory to be put output file")
    parser_convert.set_defaults(func=convert_main)

    parser_test = subparsers.add_parser("test", help="run test files")
    parser_test.set_defaults(func=test_main)

    args = parser.parse_args()
    return args

def convert_main(args):
    arg_filepath = args.filepath
    arg_output = args.output_directory

    if os.path.isfile(arg_filepath):
        filepaths = [arg_filepath]
    else:
        filepaths = glob.glob(os.path.join(arg_filepath, "**/*.py"), recursive=True)
        
    for filepath in filepaths:
        # filename = os.path.basename(filepath)
        write_filepath = get_write_filepath(filepath, os.path.relpath(arg_filepath), os.path.abspath(arg_output))

        output_dir = os.path.split(write_filepath)[0]
        if os.path.exists(output_dir):
            if not os.path.isdir(output_dir):
                raise RuntimeError("specified OUTPUT_DIRECTORY is not directory")
            else:
                pass
        else:
            os.makedirs(output_dir)

        logger.info(f"Start parsing: {filepath}")
        tree = get_ast(filepath)

        tree_has_parent_pointer = ParentPointerRewriter().visit(tree)
        tree_mock_imported = MockImporter().convert(tree_has_parent_pointer)
        logger.info(f"Mock import statement inserting has finished.")
        tree_for_else_converted = ForElseConverter().convert(tree_mock_imported)
        logger.info(f"For-else conversion has finished.")
        tree_comprehension_converted = ComprehensionConverter().convert(tree_for_else_converted)
        logger.info(f"Comprehension conversion has finished.")
        tree_for_target_converted = ForTargetConverter().convert(tree_comprehension_converted)
        logger.info(f"For target conversion has finished.")
        tree_stared_arg_converted = StaredArgConverter().convert(tree_for_target_converted)
        logger.info(f"Stared arg conversion has finished.")

        new_code = ast.unparse(ast.fix_missing_locations(tree_stared_arg_converted))

        write_file(write_filepath, new_code)
        logger.info(f"File is written to: {write_filepath}")
        print("\n")

def get_write_filepath(
        filepath: str, rel_input_path: str, abs_output_path: str) -> str:
    """
    Get the file path to write the file.

    Args:
        filepath (str): the relational path to input file
        rel_input_path (str): the relational path to specified as input path
        abs_output_path (str): the absolute path to specified as output path

    Returns:
        str: the absolute path to output file
    """
    if os.path.isfile(rel_input_path):
        filename = os.path.basename(filepath)
        write_filepath = os.path.join(abs_output_path, filename)
    else:
        filepath_from_input_path = os.path.relpath(filepath, start=rel_input_path)
        write_filepath = os.path.join(
            abs_output_path, filepath_from_input_path
        )
    return write_filepath

def test_main(args):
    argv = ["python.exe -m unittest", "discover", "-s", "test", "-p", "test_*.py", "-t", "."]
    unittest.main(module=None, argv=argv)


if __name__ == "__main__":
    args = set_arg_parser()
    args.func(args)
