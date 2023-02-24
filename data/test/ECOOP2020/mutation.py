class A:
  def __init__(self):
    self.update(0)
  def update(self, x):
    self.val = x * 2
x = A()
y = x.val
z = x
z.update('a')
if y == 10: x.atr = 'b'