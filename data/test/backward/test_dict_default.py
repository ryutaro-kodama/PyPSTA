def func(d = None):
    if d is None:
        d = {}
    x = d.get("OK")
    print(x)

func()
func({"NG": 1})
func({"OK": 0, "NG": 1})