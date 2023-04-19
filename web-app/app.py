from flask import request, abort, g
import urllib.parse as url_parser
import uuid
import time
import base64
import os
from collections.abc import Mapping
import jwt

from pagdDB_interface import PagdDBInterface
from subject_interface import SubjectInterface

SECRET_KEY = base64.b64decode(os.environ["JWT_SECRET_KEY"].encode("utf-8")) # decode the secret key stored in environment

def create_routes(app, db: PagdDBInterface, gunshot_subject: SubjectInterface):
    """ Define the endpoints for the API.
    @param app (Flask): the Flask app that handles all routes
    @param db (PagdDBInterface): an implementation of a PAGD database for storing and retrieving data
    @param gunshot_subject (SubjectInterface): an implementation of a subject for letting other apps know about changes (observer pattern)
    """
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
            g.client_id = decoded_token["id"] # store the client ID in the Flask g object
            g.expiration_date = decoded_token["exp"] # store the expiration date in the Flask g object
        except (jwt.DecodeError, KeyError) as ex:
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
        curr_time = round(time.time() * 1000) # current UNIX timestmap in milliseconds
        # information that can be extracted when decoding the token
        payload = {
            "registration": curr_time,
            "id": str(uuid.uuid4()), # generate a client ID
            "exp": curr_time + 30*24*60*60*1000 # current time + 30 days in milliseconds
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
        gun_name = data.get("gun_name") or abort(400, "required parameter gun_name was not provided")
        gun_type = data.get("gun_type") or ""
        
        result = db.add_gun(gun_name, gun_type)
        return result or abort(500, description="Failed to add the gun. Possibly duplicate entry for gun_name.")

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
        @return (json): a JSON object with the newly added report
        """
        data = request.get_json()
        timestamp = data.get("timestamp")
        coord_lat = data.get("coord_lat")
        coord_long = data.get("coord_long")
        coord_alt = data.get("coord_alt")
        gun = data.get("gun")

        if None in (timestamp, coord_lat, coord_long, coord_alt, gun):
            abort(400, "missing required parameters")

        result = db.add_report(timestamp, coord_lat, coord_long, coord_alt, gun)
        if result is not None:
            report_id = result.get("report_id")
            report = (report_id, (coord_lat, coord_long, coord_alt), timestamp, gun, g.client_id)
            gunshot_subject.notify(report)

        return result or abort(500, description="Failed to add the report.")

    @app.route("/api/reports", methods = ["GET"])
    def get_report():
        """Search for a report in the database
        @param report_id (int, optional): the report ID
        @param time_from (int, optional): UNIX timestamp of the start of the range
        @param time_to (int, optional): UNIX timestamp of the end of the range
        @return (json): a JSON object with the result
        """
        report_id = request.args.get("id")
        time_from = request.args.get("time_from", type=int)
        time_to   = request.args.get("time_to",   type=int)

        if time_from is not None and time_to is not None:
            return db.get_report_range(time_from, time_to)
        else:
            return db.get_report(report_id)

    # TODO: separate this into "/api/gunshots/temporary"
    @app.route("/api/gunshots", methods = ["POST"])
    def add_gunshot():
        """Add record of a determined gunshot event based on the given report
        @param gunshot_id (int): the gunshot ID
        @param report_id (int): the report ID which the determined gunshot is based on
        @param timestamp (int): determined UNIX timestamp of the gunshot
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param gun (string): the name of the gun
        @param shots_fired (int): the number of shots that were fired in this event
        @return json: a JSON object with the inserted value
        """
        data = request.get_json()
        gunshot_id = data.get("gunshot_id")
        report_id = data.get("report_id")
        timestamp = data.get("timestamp")
        coord_lat = data.get("coord_lat")
        coord_long = data.get("coord_long")
        coord_alt = data.get("coord_alt")
        gun = data.get("gun")
        shots_fired = data.get("shots_fired")

        if None in (gunshot_id, report_id, gun): # not enough info to add a gunshot
            abort(400, "missing required parameters")
        elif None in (timestamp, coord_lat, coord_long, coord_alt): # able to add a temporary entry
            result = db.add_temp_gunshot(gunshot_id, report_id, gun)
        else: # able to add a complete gunshot
            result = db.add_gunshot(gunshot_id, report_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired)

        return result or abort(500, description="Failed to add the gunshot. Possibly duplicate entry for gunshot_id.")

    @app.route("/api/gunshots", methods = ["PUT"])
    def update_gunshot():
        """Update the data of a gunshot with the given ID
        @param gunshot_id (int): the gunshot ID
        @param timestamp (int): determined UNIX timestamp of the gunshot
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param shots_fired (int): the number of shots that were fired in this event
        @return json: a status message
        """
        data = request.get_json()
        gunshot_id = data.get("gunshot_id")
        timestamp = data.get("timestamp")
        coord_lat = data.get("coord_lat")
        coord_long = data.get("coord_long")
        coord_alt = data.get("coord_alt")
        gun = data.get("gun")
        shots_fired = data.get("shots_fired")

        if None in (gunshot_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired): # not enough info to add a gunshot
            abort(400, "missing required parameters")
        
        result = db.update_gunshot(gunshot_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired)
        return result or abort(500, description="Failed to update the gunshot.")

    @app.route("/api/gunshots", methods = ["GET"])
    def get_gunshot():
        """Search for gunshots based on time or location (or both)
        @param gunshot_id (int, optional): the gunshot ID
        @param time_from (int, optional): UNIX timestamp of the start of the range
        @param time_to (int, optional): UNIX timestamp of the end of the range
        @return (json): a JSON object with the result
        """
        gunshot_id = request.args.get("id")
        time_from = request.args.get("time_from", type=int)
        time_to = request.args.get("time_to", type=int)

        if gunshot_id:
            return db.get_gunshot_by_id(gunshot_id)
        elif time_from and time_to:
            return db.get_gunshots_by_timestamp(time_from, time_to)
        elif time_from:
            time_now = time.time() * 1000
            return db.get_gunshots_by_timestamp(time_from, time_now)
        elif time_to:
            return db.get_gunshots_by_timestamp(0, time_to)
        
        return db.get_all_gunshots()

    @app.route("/api/gunshots/latest", methods = ["GET"])
    def get_latest_gunshot_id():
        """Get the most recent gunshot ID
        @return (int): the most recent gunshot ID
        """
        return db.get_latest_gunshot_id()
