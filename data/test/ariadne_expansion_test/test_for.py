l = [1,2,3,4,5]

for ll in l:
  print(ll)

for i in range(len(l)):
  print(l[i])

l2 = [[10, 20, 30],
      [100, 200, 300],
      [1000, 2000, 3000],
      [10000, 20000, 30000],
      [10000, 20000, 30000]]

for ll2 in l2:
  for lll2 in ll2:
    print(lll2)

print(lll2)

for x, y in zip(l, l2):
  for yy in y:
    print(x+yy)

d = {"a": 777, "b": 888, "c": 999}
for dd in d:
  print(dd)

d = {1: 111, 2: "222", "3": 333, "4": "444"}
for dd in d:
  print(dd)
