from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice

class Spline(object):
    """Class for representing B-Splines and NURBS of arbitrary degree"""

    def __init__(self, degree, knots):
        last = knots[0]
        for cur in knots[1:]:
            if cur < last:
                raise ValueError('knots not strictly increasing')
            last = cur
        self.knots = knots
        self.degree = degree

    def GetDomain(self):
        """Returns the domain of the B-Spline"""
        return (self.knots[self.degree - 1], self.knots[len(self.knots) - self.degree])

    def GetIndex(self, u):
        dom = self.GetDomain()
        for_else_iter1_pypsta = pypsta_iter(range(self.degree - 1, len(self.knots) - self.degree))
        for_else_target2_pypsta = pypsta_next(for_else_iter1_pypsta, None)
        while for_else_target2_pypsta is not None:
            ii = for_else_target2_pypsta
            if u >= self.knots[ii]:
                if u < self.knots[ii + 1]:
                    I = ii
                    break
            for_else_target2_pypsta = pypsta_next(for_else_iter1_pypsta, None)
        else:
            I = dom[1] - 1
        return I
u = int(input())
s = Spline(3, [0, 0, 0, 1, 1, 1, 2, 2, 2])
s.GetIndex(u)