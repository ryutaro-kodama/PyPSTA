
class Vector(object):

    def __init__(self, initx, inity, initz):
        self.x = initx
        self.y = inity
        self.z = initz

    def __add__(self, other):
        if other.isPoint():
            return Point(self.x + other.x, self.y + other.y, self.z + other.z)
        else:
            return Vector(self.x + other.x, self.y + other.y, self.z + other.z)

    def __sub__(self, other):
        return Vector(self.x - other.x, self.y - other.y, self.z - other.z)

    def normalized(self):
        norm = self.x * self.x + self.y * self.y + self.z * self.z
        return Vector(self.x / norm, self.y / norm, self.z / norm)

    def isPoint(self):
        return False


class Point(object):

    def __init__(self, initx, inity, initz):
        self.x = initx
        self.y = inity
        self.z = initz

    def __add__(self, other):
        return Point(self.x + other.x, self.y + other.y, self.z + other.z)

    def __sub__(self, other):
        if other.isPoint():
            return Vector(self.x - other.x, self.y - other.y, self.z - other.z)
        else:
            return Point(self.x - other.x, self.y - other.y, self.z - other.z)

    def isPoint(self):
        return True

p1 = Point(1,2,3)
p2 = Point(5,6,7)

print((p2 - p1).normalized())