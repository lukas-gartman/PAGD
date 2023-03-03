import sys
from database import Database
# import secrets
# import hashlib

class PagdDB(Database):
    def __init__(self, user, password):
        try:
            super().__init__("tottes.net", 3306, user, password, "pagd")
        except Exception: # hack-fix... should be done properly
            print("Incorrect password")
            sys.exit(1)
    
    def add_gun(self, gun_name, gun_type):
        """Add a new gun
        @param gun_name (string): the name of the gun
        @param gun_type (string): the type of gun
        @return (json): a JSON object with the newly added gun
        """
        query = "INSERT INTO Guns VALUES (%s, %s) RETURNING *;"
        rows = self.execute(query, (gun_name, gun_type))
        return self.to_json(rows)

    def get_gun(self, gun_name):
        """Search for a gun
        @param gun_name (string): the name of the gun
        @return (json): a JSON object with the result
        """
        if gun_name is not None:
            query = "SELECT * FROM Guns WHERE name = %s;"
            rows = self.execute(query, (gun_name,)) # must create a tuple
        else:
            query = "SELECT * FROM Guns;"
            rows = self.execute(query)
        return self.to_json(rows)

    def add_report(self, timestamp, coord_lat, coord_long, gun):
        """Add a report
        @param timestamp (int): the UNIX timestamp of the report
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param gun (string): the name of the gun
        @return (json): a JSON object with the newly added report
        """
        coord = f"{coord_lat},{coord_long}"
        query = "INSERT INTO Reports (timestamp, coord, gun) VALUES (FROM_UNIXTIME(%s), %s, %s) RETURNING *;"
        rows = self.execute(query, (timestamp, coord, gun))
        return self.to_json(rows, default=str)

    def get_report(self, report_id):
        """Search for a report
        @param report_id (int): the report ID
        @return (json): a JSON object with the result
        """
        # queries select the UNIX timestamp in a millisecond format (*1000) and sends data in seconds (/1000)
        if report_id is not None:
            query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Reports WHERE report_id = %s;"
            rows = self.execute(query, (report_id,)) # must create a tuple
        else:
            query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Reports;"
            rows = self.execute(query)

        return self.to_json(rows, default=int)
    
    def get_report_range(self, time_from, time_to):
        """Search for reports within a given range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int): UNIX timestamp of the beginning of the range
        @return (json): a JSON object with the result
        """
        # queries select the UNIX timestamp in a millisecond format (*1000) and sends data in seconds (/1000)
        query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Reports WHERE timestamp >= FROM_UNIXTIME(%s) AND timestamp < FROM_UNIXTIME(%s);"
        rows = self.execute(query, (time_from / 1000, time_to / 1000))
        return self.to_json(rows, default=int)

    def add_gunshot(self, timestamp, coord_lat, coord_long, gun):
        """Add record of a determined gunshot based on reports
        @param timestamp (int): average UNIX timestamp of the gunshot
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param gun (string): the name of the gun
        @return (json): a JSON object with the result
        """
        coord = f"{coord_lat},{coord_long}"
        query = "INSERT INTO Gunshots (timestamp, coord, gun) VALUES (FROM_UNIXTIME(%s), %s, %s) RETURNING *;"
        rows = self.execute(query, (timestamp / 1000, coord, gun))
        return self.to_json(rows, default=int)
    
    def get_gunshot(self, timestamp = None, coord = None):
        """Search for gunshots based on time or location (or both)
        @param timestamp (int): UNIX timestamp of the gunshot
        @param coord (string): the combined latitude and longitude coordinate
        @return (json): a JSON object with the result
        """
        result = None

        # queries select the UNIX timestamp in a millisecond format (*1000) and sends data in seconds (/1000)
        if timestamp and coord:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots WHERE timestamp = FROM_UNIXTIME(%s) AND coord = %s;"
            result = self.execute(query, (timestamp / 1000, coord))
        elif timestamp:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots WHERE timestamp = FROM_UNIXTIME(%s);"
            result = self.execute(query, (timestamp / 1000,)) # must create a tuple
        elif coord:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots WHERE coord = %s;"
            result = self.execute(query, (coord,)) # must create a tuple
        else:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots;"
            result = self.execute(query)
        
        return self.to_json(result, default=int)