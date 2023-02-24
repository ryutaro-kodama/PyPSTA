def id(x):
    return x


def listTest():
	l = [1,2,3,4]
	id(l[2])
	l[2] = 5
	id(l)

def tupleTest():
	t = (11,12,13,14)
	id(t[2])
	#t[2] = 15 		#Executing this on a tuple will cause an error
	id(t)

def setTest():
	s = {21,22,23,24}
	id(s)

def dictTest():
	d = {31:41, 32:"42", "hey" : "hi", "33":43} #right now different kinds of field instructions are used for strings and numbers
	id(d)
	id(d["hey"])
	d["hey"] = "hey"
	#d.hey = "hey"                #This line would cause the program to crash, it would however produce the same SSAInstruction as the line above
	var = d["hey"]
	id(var)


listTest()
tupleTest()
setTest()
dictTest()
