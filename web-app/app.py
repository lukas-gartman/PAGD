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

@app.route("/api/guns", methods = ["POST"])
def add_gun():
    """Add a gun to the database
    @param gun_name (string): the name of the gun
    @param gun_type (string): the type of gun
    @return json: a status message
    """
    data = request.get_json()
    gun_name = data["gun_name"]
    gun_type = data["gun_type"]
    
    db.add_gun(gun_name, gun_type)
    return {"status": "success"}

@app.route("/api/guns", methods = ["GET"])
def get_gun():
    """Search for a gun in the database
    @param gun_name (string, optional): the name of the gun
    @return json: a JSON object with the result
    """
    gun_name = request.args.get("name")
    if gun_name is not None:
        gun_name = url_parser.unquote(gun_name)
    return db.get_gun(gun_name)

@app.route("/api/reports", methods = ["POST"])
def add_report():
    """Add a report to the database
    @param timestamp (int): the UNIX timestamp of the report
    @param coord_lat (float): the latitude coordinate
    @param coord_long (float): the longitude coordinate
    @param gun (string): the name of the gun
    @return json: a status message
    """
    data = request.get_json()
    timestamp = data["timestamp"]
    coord_lat = data["coord_lat"]
    coord_long = data["coord_long"]
    gun = data["gun"]

    db.add_report(timestamp, coord_lat, coord_long, gun)
    return {"status": "success"}

@app.route("/api/reports", methods = ["GET"])
def get_report():
    """Search for a report in the database
    @param report_id (int, optional): the report ID
    @param time_from (int, optional): UNIX timestamp of the start of the range
    @param time_to (int, optional): UNIX timestamp of the beginning of the range
    @return (json): a JSON object with the result
    """
    report_id = request.args.get("id")
    time_from = request.args.get("time_from", type=int)
    time_to   = request.args.get("time_to",   type=int)

    if time_from is not None and time_to is not None:
        return db.get_report_range(time_from, time_to)
    else:
        return db.get_report(report_id)

@app.route("/api/gunshots", methods = ["POST"])
def add_gunshot():
    """Add record of a determined gunshot based on reports
    @param timestamp (int): average UNIX timestamp of the gunshot
    @param coord_lat (float): the latitude coordinate
    @param coord_long (float): the longitude coordinate
    @param gun (string): the name of the gun
    @return json: a status message
    """
    data = request.get_json()
    timestamp = data["timestamp"]
    coord_lat = data["coord_lat"]
    coord_long = data["coord_long"]
    gun = data["gun"]
    
    db.add_gunshot(timestamp, coord_lat, coord_long, gun)
    return {"status": "success"}

@app.route("/api/gunshots", methods = ["GET"])
def get_gunshot():
    """Search for gunshots based on time or location (or both)
    @param timestamp (int, optional): UNIX timestamp of the gunshot
    @param coord (string, optional): the combined latitude and longitude coordinate
    @return (json): a JSON object with the result
    """
    timestamp = request.args.get("timestamp", type=int)
    coord = request.args.get("coord")

    return db.get_gunshot(timestamp, coord)