from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
'\nSolver of Hexiom board game.\n\nBenchmark from Laurent Vaucher.\n\nSource: https://github.com/slowfrog/hexiom : hexiom2.py, level36.txt\n\n(Main function tweaked by Armin Rigo.)\n'
import io
import pyperf
DEFAULT_LEVEL = 25

class Dir(object):

    def __init__(self, x, y):
        self.x = x
        self.y = y
DIRS = [Dir(1, 0), Dir(-1, 0), Dir(0, 1), Dir(0, -1), Dir(1, 1), Dir(-1, -1)]
EMPTY = 7

class Done(object):
    MIN_CHOICE_STRATEGY = 0
    MAX_CHOICE_STRATEGY = 1
    HIGHEST_VALUE_STRATEGY = 2
    FIRST_STRATEGY = 3
    MAX_NEIGHBORS_STRATEGY = 4
    MIN_NEIGHBORS_STRATEGY = 5

    def __init__(self, count, empty=False):
        self.count = count
        compre1_pypsta = []
        for i_pypsta in range(count):
            compre1_pypsta.append([0, 1, 2, 3, 4, 5, 6, EMPTY])
        self.cells = None if empty else compre1_pypsta

    def clone(self):
        ret = Done(self.count, True)
        compre2_pypsta = []
        for i_pypsta in range(self.count):
            compre2_pypsta.append(self.cells[i_pypsta][:])
        ret.cells = compre2_pypsta
        return ret

    def __getitem__(self, i):
        return self.cells[i]

    def set_done(self, i, v):
        self.cells[i] = [v]

    def already_done(self, i):
        return len(self.cells[i]) == 1

    def remove(self, i, v):
        if v in self.cells[i]:
            self.cells[i].remove(v)
            return True
        else:
            return False

    def remove_all(self, v):
        for i in range(self.count):
            self.remove(i, v)

    def remove_unfixed(self, v):
        changed = False
        for i in range(self.count):
            if not self.already_done(i):
                if self.remove(i, v):
                    changed = True
        return changed

    def filter_tiles(self, tiles):
        for v in range(8):
            if tiles[v] == 0:
                self.remove_all(v)

    def next_cell_min_choice(self):
        minlen = 10
        mini = -1
        for i in range(self.count):
            if 1 < len(self.cells[i]) < minlen:
                minlen = len(self.cells[i])
                mini = i
        return mini

    def next_cell_max_choice(self):
        maxlen = 1
        maxi = -1
        for i in range(self.count):
            if maxlen < len(self.cells[i]):
                maxlen = len(self.cells[i])
                maxi = i
        return maxi

    def next_cell_highest_value(self):
        maxval = -1
        maxi = -1
        for i in range(self.count):
            if not self.already_done(i):
                compre3_pypsta = []
                for k_pypsta in self.cells[i]:
                    if k_pypsta != EMPTY:
                        compre3_pypsta.append(k_pypsta)
                maxvali = max(compre3_pypsta)
                if maxval < maxvali:
                    maxval = maxvali
                    maxi = i
        return maxi

    def next_cell_first(self):
        for i in range(self.count):
            if not self.already_done(i):
                return i
        return -1

    def next_cell_max_neighbors(self, pos):
        maxn = -1
        maxi = -1
        for i in range(self.count):
            if not self.already_done(i):
                cells_around = pos.hex.get_by_id(i).links
                compre4_pypsta = []
                for nid_pypsta in cells_around:
                    if self.already_done(nid_pypsta):
                        if self[nid_pypsta][0] != EMPTY:
                            compre4_pypsta.append(1)
                        else:
                            compre4_pypsta.append(0)
                    else:
                        compre4_pypsta.append(0)
                n = sum(compre4_pypsta)
                if n > maxn:
                    maxn = n
                    maxi = i
        return maxi

    def next_cell_min_neighbors(self, pos):
        minn = 7
        mini = -1
        for i in range(self.count):
            if not self.already_done(i):
                cells_around = pos.hex.get_by_id(i).links
                compre5_pypsta = []
                for nid_pypsta in cells_around:
                    if self.already_done(nid_pypsta):
                        if self[nid_pypsta][0] != EMPTY:
                            compre5_pypsta.append(1)
                        else:
                            compre5_pypsta.append(0)
                    else:
                        compre5_pypsta.append(0)
                n = sum(compre5_pypsta)
                if n < minn:
                    minn = n
                    mini = i
        return mini

    def next_cell(self, pos, strategy=HIGHEST_VALUE_STRATEGY):
        if strategy == Done.HIGHEST_VALUE_STRATEGY:
            return self.next_cell_highest_value()
        elif strategy == Done.MIN_CHOICE_STRATEGY:
            return self.next_cell_min_choice()
        elif strategy == Done.MAX_CHOICE_STRATEGY:
            return self.next_cell_max_choice()
        elif strategy == Done.FIRST_STRATEGY:
            return self.next_cell_first()
        elif strategy == Done.MAX_NEIGHBORS_STRATEGY:
            return self.next_cell_max_neighbors(pos)
        elif strategy == Done.MIN_NEIGHBORS_STRATEGY:
            return self.next_cell_min_neighbors(pos)
        else:
            raise Exception('Wrong strategy: %d' % strategy)

class Node(object):

    def __init__(self, pos, id, links):
        self.pos = pos
        self.id = id
        self.links = links

class Hex(object):

    def __init__(self, size):
        self.size = size
        self.count = 3 * size * (size - 1) + 1
        self.nodes_by_id = self.count * [None]
        self.nodes_by_pos = {}
        id = 0
        for y in range(size):
            for x in range(size + y):
                pos = (x, y)
                node = Node(pos, id, [])
                self.nodes_by_pos[pos] = node
                self.nodes_by_id[node.id] = node
                id += 1
        for y in range(1, size):
            for x in range(y, size * 2 - 1):
                ry = size + y - 1
                pos = (x, ry)
                node = Node(pos, id, [])
                self.nodes_by_pos[pos] = node
                self.nodes_by_id[node.id] = node
                id += 1

    def link_nodes(self):
        for node in self.nodes_by_id:
            (x, y) = node.pos
            for dir in DIRS:
                nx = x + dir.x
                ny = y + dir.y
                if self.contains_pos((nx, ny)):
                    node.links.append(self.nodes_by_pos[nx, ny].id)

    def contains_pos(self, pos):
        return pos in self.nodes_by_pos

    def get_by_pos(self, pos):
        return self.nodes_by_pos[pos]

    def get_by_id(self, id):
        return self.nodes_by_id[id]

class Pos(object):

    def __init__(self, hex, tiles, done=None):
        self.hex = hex
        self.tiles = tiles
        self.done = Done(hex.count) if done is None else done

    def clone(self):
        return Pos(self.hex, self.tiles, self.done.clone())

def constraint_pass(pos, last_move=None):
    changed = False
    left = pos.tiles[:]
    done = pos.done
    free_cells = range(done.count) if last_move is None else pos.hex.get_by_id(last_move).links
    for i in free_cells:
        if not done.already_done(i):
            vmax = 0
            vmin = 0
            cells_around = pos.hex.get_by_id(i).links
            for nid in cells_around:
                if done.already_done(nid):
                    if done[nid][0] != EMPTY:
                        vmin += 1
                        vmax += 1
                else:
                    vmax += 1
            for num in range(7):
                if num < vmin:
                    if done.remove(i, num):
                        changed = True
                elif num > vmax:
                    if done.remove(i, num):
                        changed = True
    for cell in done.cells:
        if len(cell) == 1:
            left[cell[0]] -= 1
    for v in range(8):
        if pos.tiles[v] > 0:
            if left[v] == 0:
                if done.remove_unfixed(v):
                    changed = True
        else:
            compre6_pypsta = []
            for cell_pypsta in done.cells:
                if v in cell_pypsta:
                    compre6_pypsta.append(1)
                else:
                    compre6_pypsta.append(0)
            possible = sum(compre6_pypsta)
            if pos.tiles[v] == possible:
                for i in range(done.count):
                    cell = done.cells[i]
                    if not done.already_done(i):
                        if v in cell:
                            done.set_done(i, v)
                            changed = True
    filled_cells = range(done.count) if last_move is None else [last_move]
    for i in filled_cells:
        if done.already_done(i):
            num = done[i][0]
            empties = 0
            filled = 0
            unknown = []
            cells_around = pos.hex.get_by_id(i).links
            for nid in cells_around:
                if done.already_done(nid):
                    if done[nid][0] == EMPTY:
                        empties += 1
                    else:
                        filled += 1
                else:
                    unknown.append(nid)
            if len(unknown) > 0:
                if num == filled:
                    for u in unknown:
                        if EMPTY in done[u]:
                            done.set_done(u, EMPTY)
                            changed = True
                elif num == filled + len(unknown):
                    for u in unknown:
                        if done.remove(u, EMPTY):
                            changed = True
    return changed
ASCENDING = 1
DESCENDING = -1

def find_moves(pos, strategy, order):
    done = pos.done
    cell_id = done.next_cell(pos, strategy)
    if cell_id < 0:
        return []
    if order == ASCENDING:
        compre7_pypsta = []
        for v_pypsta in done[cell_id]:
            compre7_pypsta.append((cell_id, v_pypsta))
        return compre7_pypsta
    else:
        compre8_pypsta = []
        for v_pypsta in done[cell_id]:
            if v_pypsta != EMPTY:
                compre8_pypsta.append((cell_id, v_pypsta))
        moves = list(reversed(compre8_pypsta))
        if EMPTY in done[cell_id]:
            moves.append((cell_id, EMPTY))
        return moves

def play_move(pos, move):
    (cell_id, i) = move
    pos.done.set_done(cell_id, i)

def print_pos(pos, output):
    hex = pos.hex
    done = pos.done
    size = hex.size
    for y in range(size):
        print(' ' * (size - y - 1), end='', file=output)
        for x in range(size + y):
            pos2 = (x, y)
            id = hex.get_by_pos(pos2).id
            if done.already_done(id):
                c = str(done[id][0]) if done[id][0] != EMPTY else '.'
            else:
                c = '?'
            print('%s ' % c, end='', file=output)
        print(end='\n', file=output)
    for y in range(1, size):
        print(' ' * y, end='', file=output)
        for x in range(y, size * 2 - 1):
            ry = size + y - 1
            pos2 = (x, ry)
            id = hex.get_by_pos(pos2).id
            if done.already_done(id):
                c = str(done[id][0]) if done[id][0] != EMPTY else '.'
            else:
                c = '?'
            print('%s ' % c, end='', file=output)
        print(end='\n', file=output)
OPEN = 0
SOLVED = 1
IMPOSSIBLE = -1

def solved(pos, output, verbose=False):
    hex = pos.hex
    tiles = pos.tiles[:]
    done = pos.done
    exact = True
    all_done = True
    for i in range(hex.count):
        if len(done[i]) == 0:
            return IMPOSSIBLE
        elif done.already_done(i):
            num = done[i][0]
            tiles[num] -= 1
            if tiles[num] < 0:
                return IMPOSSIBLE
            vmax = 0
            vmin = 0
            if num != EMPTY:
                cells_around = hex.get_by_id(i).links
                for nid in cells_around:
                    if done.already_done(nid):
                        if done[nid][0] != EMPTY:
                            vmin += 1
                            vmax += 1
                    else:
                        vmax += 1
                if num < vmin:
                    return IMPOSSIBLE
                elif num > vmax:
                    return IMPOSSIBLE
                if num != vmin:
                    exact = False
        else:
            all_done = False
    if not all_done:
        return OPEN
    elif not exact:
        return OPEN
    print_pos(pos, output)
    return SOLVED

def solve_step(prev, strategy, order, output, first=False):
    if first:
        pos = prev.clone()
        while constraint_pass(pos):
            pass
    else:
        pos = prev
    moves = find_moves(pos, strategy, order)
    if len(moves) == 0:
        return solved(pos, output)
    else:
        for move in moves:
            ret = OPEN
            new_pos = pos.clone()
            play_move(new_pos, move)
            while constraint_pass(new_pos, move[0]):
                pass
            cur_status = solved(new_pos, output)
            if cur_status != OPEN:
                ret = cur_status
            else:
                ret = solve_step(new_pos, strategy, order, output)
            if ret == SOLVED:
                return SOLVED
    return IMPOSSIBLE

def check_valid(pos):
    hex = pos.hex
    tiles = pos.tiles
    tot = 0
    for i in range(8):
        if tiles[i] > 0:
            tot += tiles[i]
        else:
            tiles[i] = 0
    if tot != hex.count:
        raise Exception('Invalid input. Expected %d tiles, got %d.' % (hex.count, tot))

def solve(pos, strategy, order, output):
    check_valid(pos)
    return solve_step(pos, strategy, order, output, first=True)

def read_file(file):
    compre9_pypsta = []
    for line_pypsta in file.splitlines():
        compre9_pypsta.append(line_pypsta.strip('\r\n'))
    lines = compre9_pypsta
    size = int(lines[0])
    hex = Hex(size)
    linei = 1
    tiles = 8 * [0]
    done = Done(hex.count)
    for y in range(size):
        line = lines[linei][size - y - 1:]
        p = 0
        for x in range(size + y):
            tile = line[p:p + 2]
            p += 2
            if tile[1] == '.':
                inctile = EMPTY
            else:
                inctile = int(tile)
            tiles[inctile] += 1
            if tile[0] == '+':
                done.set_done(hex.get_by_pos((x, y)).id, inctile)
        linei += 1
    for y in range(1, size):
        ry = size - 1 + y
        line = lines[linei][y:]
        p = 0
        for x in range(y, size * 2 - 1):
            tile = line[p:p + 2]
            p += 2
            if tile[1] == '.':
                inctile = EMPTY
            else:
                inctile = int(tile)
            tiles[inctile] += 1
            if tile[0] == '+':
                done.set_done(hex.get_by_pos((x, ry)).id, inctile)
        linei += 1
    hex.link_nodes()
    done.filter_tiles(tiles)
    return Pos(hex, tiles, done)

def solve_file(file, strategy, order, output):
    pos = read_file(file)
    solve(pos, strategy, order, output)
LEVELS = {}
LEVELS[2] = ('\n2\n  . 1\n . 1 1\n  1 .\n', ' 1 1\n. . .\n 1 1\n')
LEVELS[10] = ('\n3\n  +.+. .\n +. 0 . 2\n . 1+2 1 .\n  2 . 0+.\n   .+.+.\n', '  . . 1\n . 1 . 2\n0 . 2 2 .\n . . . .\n  0 . .\n')
LEVELS[20] = ('\n3\n   . 5 4\n  . 2+.+1\n . 3+2 3 .\n +2+. 5 .\n   . 3 .\n', '  3 3 2\n 4 5 . 1\n3 5 2 . .\n 2 . . .\n  . . .\n')
LEVELS[25] = ('\n3\n   4 . .\n  . . 2 .\n 4 3 2 . 4\n  2 2 3 .\n   4 2 4\n', '  3 4 2\n 2 4 4 .\n. . . 4 2\n . 2 4 3\n  . 2 .\n')
LEVELS[30] = ('\n4\n    5 5 . .\n   3 . 2+2 6\n  3 . 2 . 5 .\n . 3 3+4 4 . 3\n  4 5 4 . 5 4\n   5+2 . . 3\n    4 . . .\n', '   3 4 3 .\n  4 6 5 2 .\n 2 5 5 . . 2\n. . 5 4 . 4 3\n . 3 5 4 5 4\n  . 2 . 3 3\n   . . . .\n')
LEVELS[36] = ('\n4\n    2 1 1 2\n   3 3 3 . .\n  2 3 3 . 4 .\n . 2 . 2 4 3 2\n  2 2 . . . 2\n   4 3 4 . .\n    3 2 3 3\n', '   3 4 3 2\n  3 4 4 . 3\n 2 . . 3 4 3\n2 . 1 . 3 . 2\n 3 3 . 2 . 2\n  3 . 2 . 2\n   2 2 . 1\n')

def main(loops, level):
    (board, solution) = LEVELS[level]
    order = DESCENDING
    strategy = Done.FIRST_STRATEGY
    stream = io.StringIO()
    board = board.strip()
    expected = solution.rstrip()
    range_it = range(loops)
    t0 = pyperf.perf_counter()
    for _ in range_it:
        stream = io.StringIO()
        solve_file(board, strategy, order, stream)
        output = stream.getvalue()
        stream = None
    dt = pyperf.perf_counter() - t0
    compre10_pypsta = []
    for line_pypsta in output.splitlines():
        compre10_pypsta.append(line_pypsta.rstrip())
    output = '\n'.join(compre10_pypsta)
    if output != expected:
        raise AssertionError('got a wrong answer:\n%s\nexpected: %s' % (output, expected))
    return dt

def add_cmdline_args(cmd, args):
    cmd.extend(('--level', str(args.level)))
if __name__ == '__main__':
    kw = {'add_cmdline_args': add_cmdline_args}
    if pyperf.python_has_jit():
        kw['warmups'] = 15
    runner = pyperf.Runner(**kw)
    levels = sorted(LEVELS)
    runner.argparser.add_argument('--level', type=int, choices=levels, default=DEFAULT_LEVEL, help='Hexiom board level (default: %s)' % DEFAULT_LEVEL)
    args = runner.parse_args()
    runner.metadata['description'] = 'Solver of Hexiom board game'
    runner.metadata['hexiom_level'] = args.level
    runner.bench_time_func('hexiom', main, args.level)