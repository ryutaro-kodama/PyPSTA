from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
from __future__ import print_function, unicode_literals, division
import os
import sys
if sys.version < '3':
    str = unicode

def match(needle, haystack):
    """
    Command-T-style string matching.
    """
    score = 1
    j = 0
    last_match = 0
    needle = needle.lower()
    haystack = haystack.lower()
    for c in needle:
        while j < len(haystack) and haystack[j] != c:
            j += 1
        if j >= len(haystack):
            return 0
        score += 1 / (last_match + 1.0)
        last_match = j
        j += 1
    return score

pypsta_default_arg_temp1 = sys.stdout
def print_table(matrix, sep='  ', file=pypsta_default_arg_temp1, *args, **kwargs):
    """
    Prints a left-aligned table of elements.
    """
    compre1_pypsta = []
    for column_pypsta in zip(*matrix, pypsta_stared_arg1=matrix):
        compre1_pypsta.append(max(map(len, map(str, column_pypsta))))
    lengths = compre1_pypsta
    compre2_pypsta = []
    for for_target1_pypsta in enumerate(lengths):
        (i_pypsta, length_pypsta) = for_target1_pypsta
        compre2_pypsta.append('{{{0}:<{1}}}'.format(i_pypsta, length_pypsta))
    format = sep.join(compre2_pypsta)
    for row in matrix:
        print(format.format(*row).strip(), *args, file=file, **kwargs, pypsta_stared_arg2=args)

def db2dbm(quality):
    """
    Converts the Radio (Received) Signal Strength Indicator (in db) to a dBm
    value.  Please see http://stackoverflow.com/a/15798024/1013960
    """
    dbm = int(quality / 2 - 100)
    return min(max(dbm, -100), -50)

def ensure_file_exists(filename):
    """
    http://stackoverflow.com/a/12654798/1013960
    """
    if not os.path.exists(filename):
        open(filename, 'a').close()