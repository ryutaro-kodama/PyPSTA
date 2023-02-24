def print1():
    print("1")

def print2():
    print("2")

def print3():
    print("3")

class Task:
    def __init__(self, call_back):
        self.call_back = call_back

    def fn(self):
        raise NotImplementedError

    def runTask(self):
        return self.fn(self.call_back)

class DeviceTask(Task):
    def __init__(self, call_back):
        Task.__init__(self, call_back)

    def fn(self, call_back):
        call_back()

class HandlerTask(Task):
    def __init__(self, call_back):
        Task.__init__(self, call_back)

    def fn(self, call_back):
        call_back()

def schedule():
    taskList = [DeviceTask(print1), DeviceTask(print2), HandlerTask(print3)]
    for t in taskList:
        t.runTask()

schedule()