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
        result = self.execute(query, (gun_name, gun_type))
        return self.to_json(result)

    def get_gun(self, gun_name):
        """Search for a gun
        @param gun_name (string): the name of the gun
        @return (json): a JSON object with the result
        """
        if gun_name is not None:
            query = "SELECT * FROM Guns WHERE name = %s LIMIT 1;"
            result = self.execute(query, (gun_name,)) # must create a tuple
        else:
            query = "SELECT * FROM Guns;"
            result = self.execute(query)
        return self.to_json(result)

    def add_report(self, timestamp, coord_lat, coord_long, coord_alt, gun):
        """Add a report
        @param timestamp (int): the UNIX timestamp of the report
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param gun (string): the name of the gun
        @return (json): a JSON object with the newly added report
        """
        if coord_alt:
            query = "INSERT INTO Reports (timestamp, coord_lat, coord_long, coord_alt, gun) VALUES (FROM_UNIXTIME(%s), %s, %s, %s, %s) RETURNING *;"
            result = self.execute(query, (timestamp / 1000, coord_lat, coord_long, coord_alt, gun))
        else:
            query = "INSERT INTO Reports (timestamp, coord_lat, coord_long, gun) VALUES (FROM_UNIXTIME(%s), %s, %s, %s) RETURNING *;"
            result = self.execute(query, (timestamp / 1000, coord_lat, coord_long, gun))

        return self.to_json(result, default=str)

    def get_report(self, report_id):
        """Search for a report
        @param report_id (int): the report ID
        @return (json): a JSON object with the result
        """
        # queries select the UNIX timestamp in a millisecond format (*1000) and sends data in seconds (/1000)
        if report_id is not None:
            query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord_lat, coord_long, coord_alt, gun FROM Reports WHERE report_id = %s;"
            result = self.execute(query, (report_id,)) # must create a tuple
        else:
            query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord_lat, coord_long, coord_alt, gun FROM Reports;"
            result = self.execute(query)

        return self.to_json(result, default=int)
    
    def get_report_range(self, time_from, time_to):
        """Search for reports within a given range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int): UNIX timestamp of the beginning of the range
        @return (json): a JSON object with the result
        """
        # queries select the UNIX timestamp in a millisecond format (*1000) and sends data in seconds (/1000)
        query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord_lat, coord_long, coord_alt, gun FROM Reports WHERE timestamp >= FROM_UNIXTIME(%s) AND timestamp < FROM_UNIXTIME(%s);"
        result = self.execute(query, (time_from / 1000, time_to / 1000))
        return self.to_json(result, default=int)

    def add_gunshot(self, gunshot_id, report, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired):
        """Add record of a determined gunshot event based on the given report
        @param gunshot_id (int): the gunshot ID
        @param report (int): the report ID which the determined gunshot is based on
        @param timestamp (int): determined UNIX timestamp of the gunshot
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param gun (string): the name of the gun
        @param shots_fired (int): the number of shots that were fired in this event
        @return json: a JSON object with the inserted value
        """
        if coord_alt:
            query = "INSERT INTO GunshotEvents (gunshot_id, report, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired) VALUES (%s, %s, FROM_UNIXTIME(%s), %s, %s, %s, %s, %s) RETURNING *;"
            result = self.execute(query, (gunshot_id, report, timestamp / 1000, coord_lat, coord_long, coord_alt, gun, shots_fired))
        else:
            query = "INSERT INTO GunshotEvents (gunshot_id, report, timestamp, coord_lat, coord_long, gun, shots_fired) VALUES (%s, %s, FROM_UNIXTIME(%s), %s, %s, %s, %s) RETURNING *;"
            result = self.execute(query, (gunshot_id, report, timestamp / 1000, coord_lat, coord_long, gun, shots_fired))
        
        return self.to_json(result, default=str)
    
    def add_temp_gunshot(self, gunshot_id, report, gun):
        """Add a temporary placeholder record for a gunshot event. This record will later be updated when there are more reports to process.
        @param gunshot_id (int): the gunshot ID
        @param report (int): the report ID
        @param gun (string): the name of the gun
        @return json: a JSON object with the inserted value
        """
        query = "INSERT INTO GunshotEvents (gunshot_id, report, gun) VALUES (%s, %s, %s) RETURNING *;"
        result = self.execute(query, (gunshot_id, report, gun))
        return self.to_json(result, default=str)
    
    def update_gunshot(self, gunshot_id, timestamp, coord_lat, coord_long, coord_alt, shots_fired):
        """Update the data of the gunshot with the given ID
        @param gunshot_id (int): the gunshot ID
        @param timestamp (int): determined UNIX timestamp of the gunshot
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param shots_fired (int): the number of shots that were fired in this event
        @return json: a JSON object with the inserted value
        """
        query = "UPDATE GunshotEvents SET (timestamp = %s, coord_lat = %s, coord_long = %s, coord_alt = %s, shots_fired = %s) WHERE gunshot_id = %s;"
        result = self.execute(query, (timestamp, coord_lat, coord_long, coord_alt, shots_fired, gunshot_id))
        return self.to_json(result, default=str)
    
    def get_gunshot_by_id(self, gunshot_id):
        """Search for gunshots based on time or location (or both)
        @param gunshot_id (int): the gunshot ID
        @return (json): a JSON object with the result
        """
        query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired FROM GunshotEvents WHERE gunshot_id = %s LIMIT 1;"
        result = self.execute(query, (gunshot_id,)) # must create a tuple
        return self.to_json(result, default=int)
    
    def get_gunshots_by_timestamp(self, time_from, time_to):
        """Search for gunshots within the given time range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int):   UNIX timestamp of the start of the range
        @return (json): a JSON object with the result
        """
        # queries select the UNIX timestamp in a millisecond format (*1000) and sends data in seconds (/1000)
        query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired FROM GunshotEvents WHERE timestamp >= FROM_UNIXTIME(%s) AND timestamp < FROM_UNIXTIME(%s);"
        result = self.execute(query, (time_from / 1000, time_to / 1000))
        return self.to_json(result, default=int)
    
    def get_all_gunshots(self):
        """Retrieve all gunshots
        @return (json): a JSON object with the result
        """
        query = "SELECT gunshot_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired FROM GunshotEvents;"
        result = self.execute(query)
        return self.to_json(result, default=int)

    # TODO: def get_gunshot_by_radius(self, midpoint_coord, radius):
    
    def get_latest_gunshot_id(self):
        """Get the most recent gunshot ID
        @return (int): the most recent gunshot ID
        """
        query = "SELECT MAX(gunshot_id) FROM GunshotEvents;"
        return self.execute(query)
