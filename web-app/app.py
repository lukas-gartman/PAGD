import sys
import json
from flask import Flask, request
from getpass import getpass
import urllib.parse as url_parser

from pagdDB import PagdDB

app = Flask(__name__)
try:
    db = PagdDB("pagd", getpass("Database password: "))
except Exception: # hack-fix... should be done properly
   print("Incorrect password")
   sys.exit(1)

@app.route("/")
def hello_world():
    return {
        "message": "Hello, World!"
    }

@app.route("/api/guns/all")
def get_all_guns():
    all_guns = db.get_all_guns()
    json_output = json.dumps(all_guns)
    return json_output

@app.route("/api/guns", methods = ["POST"])
def add_gun():
    data = request.get_json()
    gun_name = data["gun_name"]
    gun_type = data["gun_type"]
    
    if db.add_gun(gun_name, gun_type):
        return {"status": "success"}
    else:
        return {"status": "failed to add gun"}

@app.route("/api/guns", methods = ["GET"])
def search_gun():
    gun_name = url_parser.unquote(request.args.get("name"), "")
    result = db.get_gun(gun_name)
    return json.dumps(result)

@app.route("/api/reports", methods = ["POST"])
def add_report():
    data = request.get_json()
    timestamp = data["timestamp"]
    coord_lat = data["coord_lat"]
    coord_long = data["coord_long"]
    gun = data["gun"]

    db.add_report(timestamp, coord_lat, coord_long, gun)
    return {"status": "success"}

@app.route("/api/reports", methods = ["GET"])
def get_report():
    report_id = request.args.get("id")
    time_from = request.args.get("time_from", type=int)
    time_to   = request.args.get("time_to",   type=int)

    if time_from and time_to:
        result = db.get_report_range(time_from, time_to)
    else:
        result = db.get_report(report_id)
    return json.dumps(result, default=int)

@app.route("/api/gunshots", methods = ["POST"])
def add_gunshot():
    data = request.get_json()
    timestamp = data["timestamp"]
    coord_lat = data["coord_lat"]
    coord_long = data["coord_long"]
    gun = data["gun"]
    
    db.add_gunshot(timestamp, coord_lat, coord_long, gun)
    return {"status": "success"}

@app.route("/api/gunshots", methods = ["GET"])
def get_gunshot():
    timestamp = request.args.get("timestamp", type=int)
    coord = request.args.get("coord")

    result = db.get_gunshot(timestamp, coord)
    print(result)
    return json.dumps(result, default=int)