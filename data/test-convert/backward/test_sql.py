from pypsta_mock import range
from pypsta_mock import pypsta_iter
from pypsta_mock import pypsta_next
from pypsta_mock import pypsta_slice
SELECT = 0
CREATE = 1

class Create:

    def run(self):
        print('RUN SQL')

class Select:

    def run(self):
        print('RUN SQL')

    def add_where(self):
        print('ADD WHERE')

def run_sql(mode):
    if mode == CREATE:
        sql = Create()
    else:
        sql = Select()
    if mode == SELECT:
        sql.add_where()
    sql.run()
import random
run_sql(random.randrange(0, 2))