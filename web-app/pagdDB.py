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
        query = "INSERT INTO Reports (timestamp, coord_lat, coord_long, coord_alt, gun) VALUES (%s, %s, %s, %s, %s) RETURNING *;"
        result = self.execute(query, (timestamp, coord_lat, coord_long, coord_alt, gun))
        return self.to_json(result, default=str)

    def get_report(self, report_id):
        """Search for a report
        @param report_id (int): the report ID
        @return (json): a JSON object with the result
        """
        if report_id is not None:
            query = "SELECT * FROM Reports WHERE report_id = %s;"
            result = self.execute(query, (report_id,)) # must create a tuple
        else:
            query = "SELECT * FROM Reports;"
            result = self.execute(query)

        return self.to_json(result, default=int)
    
    def get_report_range(self, time_from, time_to):
        """Search for reports within a given range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int): UNIX timestamp of the beginning of the range
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Reports WHERE timestamp >= %s AND timestamp < %s;"
        result = self.execute(query, (time_from, time_to))
        return self.to_json(result, default=int)

    def add_gunshot(self, gunshot_id, report_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired):
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
        queries = []
        values  = []

        # Add the gunshot and update if it already exists
        queries.append("""INSERT INTO Gunshots VALUES (%s, %s, %s, %s, %s, %s, %s) ON DUPLICATE KEY UPDATE
            timestamp = VALUES(timestamp), coord_lat = VALUES(coord_lat), coord_long = VALUES(coord_long), coord_alt = VALUES(coord_alt),
            gun = VALUES(gun), shots_fired = VALUES(shots_fired)
            RETURNING *;""")
        values.append((gunshot_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired))
        
        # Add the relation
        queries.append("INSERT INTO GunshotReports VALUES (%s, %s) RETURNING *;")
        values.append((gunshot_id, report_id))

        result = self.execute_transaction(queries, values)
        if result:
            return self.to_json(result[0], default=str)
        else:
            return None
    
    def add_temp_gunshot(self, gunshot_id, report_id, gun):
        """Add a temporary placeholder record for a gunshot event. This record will later be updated when there are more reports to process.
        @param gunshot_id (int): the gunshot ID
        @param report_id (int): the report ID
        @param gun (string): the name of the gun
        @return json: a JSON object with the inserted value
        """
        queries = []
        values  = []

        # Add the gunshot
        queries.append("INSERT INTO Gunshots (gunshot_id, gun) VALUES (%s, %s) ON DUPLICATE KEY UPDATE gun = VALUES(gun) RETURNING *;")
        values.append((gunshot_id, gun))
        
        # Add the relation
        queries.append("INSERT INTO GunshotReports VALUES (%s, %s) RETURNING *;")
        values.append((gunshot_id, report_id))

        result = self.execute_transaction(queries, values)
        return self.to_json(result[0], default=str)
    
    # def add_gunshot_report(self, gunshot_id, report_id):
    #     """Add a gunshot report relation
    #     @param gunshot_id (int): the gunshot ID
    #     @param report_id (int): the report ID
    #     @return json: a JSON object with the inserted value
    #     """
    
    def update_gunshot(self, gunshot_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired):
        """Update the data of the gunshot with the given ID
        @param gunshot_id (int): the gunshot ID
        @param timestamp (int): determined UNIX timestamp of the gunshot
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param gun (string): the name of the gun
        @param shots_fired (int): the number of shots that were fired in this event
        @return json: a JSON object with the inserted value
        """
        query = "UPDATE Gunshots SET timestamp = %s, coord_lat = %s, coord_long = %s, coord_alt = %s, gun = %s, shots_fired = %s WHERE gunshot_id = %s;"
        result = self.execute(query, (timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired, gunshot_id))
        return self.to_json(result, default=str)
    
    def get_gunshot_by_id(self, gunshot_id):
        """Search for gunshots based on time or location (or both)
        @param gunshot_id (int): the gunshot ID
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Gunshots WHERE gunshot_id = %s;"
        result = self.execute(query, (gunshot_id,)) # must create a tuple
        return self.to_json(result, default=int)
    
    def get_gunshots_by_timestamp(self, time_from, time_to):
        """Search for gunshots within the given time range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int):   UNIX timestamp of the start of the range
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Gunshots WHERE timestamp >= %s AND timestamp < %s;"
        result = self.execute(query, (time_from, time_to))
        return self.to_json(result, default=int)
    
    def get_all_gunshots(self):
        """Retrieve all gunshots
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Gunshots;"
        result = self.execute(query)
        return self.to_json(result, default=int)

    # TODO: def get_gunshot_by_radius(self, midpoint_coord, radius):
    
    def get_latest_gunshot_id(self):
        """Get the most recent gunshot ID
        @return (int): the most recent gunshot ID
        """
        query = "SELECT MAX(gunshot_id) AS gunshot_id FROM Gunshots;"
        result = self.execute(query)
        return self.to_json(result, default=int)
