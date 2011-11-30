from flask import Flask, g, request
from cherrypy import wsgiserver
from sqlite3 import connect

app = Flask(__name__)

def create_tables():
  db = connect("./mailbox.sqlite")
  c = db.cursor()
  c.execute("drop table if exists clients")
  c.execute("drop table if exists balls")
  c.execute("""
    create table clients (
    cid integer primary key autoincrement,
    w integer,
    h integer)
    """)
  c.execute("""
    create table balls (
      cid integer,
      y real,
      vx real,
      vy real,
      side integer,
	  colour integer)
    """)
  db.close()

@app.before_request
def before_request():
  g.db = connect("./mailbox.sqlite")

@app.teardown_request
def teardown_request(exception):
  g.db.commit()
  g.db.close()

@app.route("/")
def index():
  return """
  <pre>
  /register ? w= & h= &
    returns cid
  /update ? cid= & y= & vx= & vy= & side=
    returns -1/0
  /ping ? cid=
    returns list of balls
  </pre>
  """
@app.route("/register", methods=["POST", "GET"])
def register():
  try:
    width = int(request.args.get("w"))
    height = int(request.args.get('h'))
  except:
    return "-1"
	
  #print "got here"

  c = g.db.cursor()
  c.execute("insert into clients (w, h) values (?,?)", 
            (width, height))
  cid = c.lastrowid
  return str(cid)

@app.route("/update", methods=["POST", "GET"])
def update():
  try:
    cid = int(request.args.get("cid"))
    y = float(request.args.get("y"))
    vx = float(request.args.get("vx"))
    vy = float(request.args.get("vy"))
    side = int(request.args.get("side"))
    colour = int(request.args.get("colour"))
  except:
    return "-1"

  c = g.db.cursor()
  compare = "<" if side < 0 else ">"
  order = "desc" if side < 0 else "asc"
  c.execute("""
      select cid from clients where cid %s ? 
      order by cid %s limit 1""" % (compare, order), (cid,))
  results = c.fetchall()
  if not results:
    if side < 0:
      c.execute("select max(cid) from clients")
    else:
      c.execute("select min(cid) from clients")
    results = c.fetchall()

  # Put the ball into the balls table
  nid = results[0][0]
  print "putting into nid=", nid
  c.execute("insert into balls values (?,?,?,?,?,?)", (nid,y,vx,vy,side,colour))
  return "0"

@app.route("/ping", methods=["POST", "GET"])
def ping():
  try:
    cid = int(request.args.get("cid"))
  except:
    return "-1"

  c = g.db.cursor()
  c.execute("select y, vx, vy, side, colour from balls where cid = ?", (cid,))
  result = "\n".join(",".join(map(str, row)) for row in c.fetchall())
  c.execute("delete from balls where cid=?", (cid,))
  return result


if __name__ == '__main__':
  import sys
  if sys.argv[1:] and sys.argv[1] == 'create':
    create_tables()
    sys.exit()

  if sys.argv[1:] and sys.argv[1] == 'debug':
    app.run(host='0.0.0.0', port=15001, debug=True)
  else:
    d = wsgiserver.WSGIPathInfoDispatcher({"/": app})
    server = wsgiserver.CherryPyWSGIServer(('0.0.0.0', 15001), d)
    try:
      server.start()
    except KeyboardInterrupt:
      server.stop()
