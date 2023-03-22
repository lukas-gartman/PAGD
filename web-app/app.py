import json
from flask import Flask, request, abort
from getpass import getpass
import urllib.parse as url_parser
import time
import base64
import os
from collections.abc import Mapping
import jwt

from pagdDB import PagdDB

app = Flask(__name__)
db = PagdDB("pagd", getpass("Database password: "))
SECRET_KEY = base64.b64decode(os.environ["JWT_SECRET_KEY"].encode("utf-8")) # decode the secret key stored in environment

@app.before_request
def auth():
    """Authenticate user for each API call by verifying their JWT token in the Authorization header
    @param Authorization (string): the JWT token included in the Authorization header
    """
    # no authentication needed for anything other than the API
    if not request.path.startswith("/api"):
        return
    
    token = request.headers.get("Authorization")
    if not token: # token not provided
        abort(401, description="No Authorization header was provided.")
    
    try:
        decoded_token = jwt.decode(token, SECRET_KEY, "HS256")
    except jwt.DecodeError as ex:
        print("Invalid token:", str(ex))
        abort(401, description=f"Invalid token: {str(ex)}")

@app.route("/")
def hello_world():
    return {
        "message": "Hello, World!"
    }

@app.route("/register")
def register():
    """Retrieve a JWT token used to authorize API calls. Include the token in the Authorization header.
    @return (json): the JWT token containing the registration date timestamp in its payload
    """
    # information that can be extracted when decoding the token
    payload = {
        "registration": round(time.time() * 1000) # current UNIX timestmap in milliseconds
        # TODO: add "exp" for expiration date
    }
    jwt_token = jwt.encode(payload, SECRET_KEY, "HS256") # create the JWT token

    return {"token": jwt_token}

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
    @param coord_alt (float): the altitude coordinate
    @param gun (string): the name of the gun
    @return json: a status message
    """
    data = request.get_json()
    timestamp = data["timestamp"]
    coord_lat = data["coord_lat"]
    coord_long = data["coord_long"]
    coord_alt = data["coord_alt"]
    gun = data["gun"]

    db.add_report(timestamp, coord_lat, coord_long, coord_alt, gun)
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
    @param coord_alt (float): the altitude coordinate
    @param gun (string): the name of the gun
    @param report (int): the report ID
    @return json: a status message
    """
    data = request.get_json()
    timestamp = data["timestamp"]
    coord_lat = data["coord_lat"]
    coord_long = data["coord_long"]
    coord_alt = data["coord_alt"]
    gun = data["gun"]
    report = data["report"]
    
    db.add_gunshot(timestamp, coord_lat, coord_long, coord_alt, gun, report)
    return {"status": "success"}

@app.route("/api/gunshots", methods = ["GET"])
def get_gunshot():
    """Search for gunshots based on time or location (or both)
    @param timestamp (int, optional): UNIX timestamp of the gunshot
    @param coord (string, optional): the combined latitude and longitude coordinate
    @return (json): a JSON object with the result
    """
    timestamp = request.args.get("timestamp", type=int)
    coord_lat = request.args.get("coord_lat")
    coord_long = request.args.get("coord_long")
    coord_alt = request.args.get("coord_alt")

    return db.get_gunshot(timestamp, coord_lat, coord_long, coord_alt)