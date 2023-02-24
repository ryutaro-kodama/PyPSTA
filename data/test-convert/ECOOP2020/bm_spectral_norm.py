from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
'\nMathWorld: "Hundred-Dollar, Hundred-Digit Challenge Problems", Challenge #3.\nhttp://mathworld.wolfram.com/Hundred-DollarHundred-DigitChallengeProblems.html\n\nThe Computer Language Benchmarks Game\nhttp://benchmarksgame.alioth.debian.org/u64q/spectralnorm-description.html#spectralnorm\n\nContributed by Sebastien Loisel\nFixed by Isaac Gouy\nSped up by Josh Goldfoot\nDirtily sped up by Simon Descarpentries\nConcurrency by Jason Stitt\n'
import pyperf
DEFAULT_N = 130

def eval_A(i, j):
    return 1.0 / ((i + j) * (i + j + 1) // 2 + i + 1)

def eval_times_u(func, u):
    compre1_pypsta = []
    for i_pypsta in range(len(list(u))):
        compre1_pypsta.append(func((i_pypsta, u)))
    return compre1_pypsta

def eval_AtA_times_u(u):
    return eval_times_u(part_At_times_u, eval_times_u(part_A_times_u, u))

def part_A_times_u(i_u):
    (i, u) = i_u
    partial_sum = 0
    for for_target1_pypsta in enumerate(u):
        (j, u_j) = for_target1_pypsta
        partial_sum += eval_A(i, j) * u_j
    return partial_sum

def part_At_times_u(i_u):
    (i, u) = i_u
    partial_sum = 0
    for for_target2_pypsta in enumerate(u):
        (j, u_j) = for_target2_pypsta
        partial_sum += eval_A(j, i) * u_j
    return partial_sum

def bench_spectral_norm(loops):
    range_it = range(loops)
    t0 = pyperf.perf_counter()
    for _ in range_it:
        u = [1] * DEFAULT_N
        for dummy in range(10):
            v = eval_AtA_times_u(u)
            u = eval_AtA_times_u(v)
        vBv = vv = 0
        for for_target3_pypsta in zip(u, v):
            (ue, ve) = for_target3_pypsta
            vBv += ue * ve
            vv += ve * ve
    return pyperf.perf_counter() - t0
if __name__ == '__main__':
    runner = pyperf.Runner()
    runner.metadata['description'] = 'MathWorld: "Hundred-Dollar, Hundred-Digit Challenge Problems", Challenge #3.'
    runner.bench_time_func('spectral_norm', bench_spectral_norm)