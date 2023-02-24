SELECT = 0
CREATE = 1

class Create:
  def run(self):
    print("RUN SQL")

class Select:
  def run(self):
    print("RUN SQL")

  def add_where(self):
    print("ADD WHERE")

def run_sql(mode):
  # `mode` is `CREATE` or `SELECT`
  if mode == CREATE:
    sql = Create()
  else:
    sql = Select()

  if mode == SELECT:
    sql.add_where()  # never called for 'Create'

  sql.run()

import random
run_sql(random.randrange(0, 2))
