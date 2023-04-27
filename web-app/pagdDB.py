import sys
import json
from database import Database
from pagdDB_interface import PagdDBInterface

class PagdDB(Database, PagdDBInterface):
    def __init__(self, user, password):
        try:
            super().__init__("localhost", 3306, user, password, "pagd")
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
        try:
            result = self.execute(query, (gun_name, gun_type))
        except:
            return None
        
        return self.to_json(*result)

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
        return self.to_json(*result)

    def add_report(self, timestamp, coord_lat, coord_long, coord_alt, gun, client_id):
        """Add a report
        @param timestamp (int): the UNIX timestamp of the report
        @param coord_lat (float): the latitude coordinate
        @param coord_long (float): the longitude coordinate
        @param coord_alt (float): the altitude coordinate
        @param gun (string): the name of the gun
        @param client_id (string): the client's unique ID
        @return (json): a JSON object with the newly added report
        """
        query = "INSERT INTO Reports (timestamp, coord_lat, coord_long, coord_alt, gun, client_id) VALUES (%s, %s, %s, %s, %s, %s) RETURNING *;"
        try:
            result = self.execute(query, (timestamp, coord_lat, coord_long, coord_alt, gun, client_id))
        except Exception as e:
            print("add_report (pagdDB.py):", str(e))
            return None
        return self.to_json(*result, default=str)
    
    def add_reports(self, values):
        query = "INSERT INTO Reports (timestamp, coord_lat, coord_long, coord_alt, gun, client_id) VALUES (%s, %s, %s, %s, %s, %s) RETURNING *;"
        try:
            result = self.execute(query, values)
        except Exception as e:
            print("add_reports (pagdDB.py):", str(e))
            return None
        return self.to_json(*result, default=str)

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

        return self.to_json(*result, default=int)
    
    def get_report_range(self, time_from, time_to):
        """Search for reports within a given range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int): UNIX timestamp of the beginning of the range
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Reports WHERE timestamp >= %s AND timestamp < %s;"
        result = self.execute(query, (time_from, time_to))
        return self.to_json(*result, default=int)

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

        try:
            result = self.execute_transaction(queries, values)
        except:
            return None
        return self.to_json(result[0], default=str) or None
        
    
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

        try:
            result = self.execute_transaction(queries, values)
        except:
            return None
        return self.to_json(result[0], default=str) or None
    
    def add_gunshot_report_relation(self, gunshot_id, report_id):
        """Add a gunshot report relation
        @param gunshot_id (int): the gunshot ID
        @param report_id (int): the report ID
        @return json: a JSON object with the inserted value
        """
        query = "INSERT INTO GunshotReports VALUES (%s, %s) RETURNING *;"
        try:
            result = self.execute(query, (gunshot_id, report_id))
        except:
            return None
        return self.to_json(*result)
    
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
        queries = []
        values = []
        
        queries.append("UPDATE Gunshots SET timestamp = %s, coord_lat = %s, coord_long = %s, coord_alt = %s, gun = %s, shots_fired = %s WHERE gunshot_id = %s;")
        values.append((timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired, gunshot_id))
        
        queries.append("SELECT * FROM Gunshots WHERE gunshot_id = %s;") # RETURNING * is not supported for UPDATE queries, therefore you must select
        values.append((gunshot_id,))

        try:
            result = self.execute_transaction(queries, values)
        except:
            return None
        return self.to_json(result[1], default=str) or None
    
    def get_gunshot_by_id(self, gunshot_id):
        """Search for gunshots based on time or location (or both)
        @param gunshot_id (int): the gunshot ID
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Gunshots WHERE gunshot_id = %s;"
        result = self.execute(query, (gunshot_id,)) # must create a tuple
        return self.to_json(*result, default=int)
    
    def get_gunshots_by_timestamp(self, time_from, time_to):
        """Search for gunshots within the given time range
        @param time_from (int): UNIX timestamp of the start of the range
        @param time_to (int):   UNIX timestamp of the start of the range
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Gunshots WHERE timestamp >= %s AND timestamp < %s;"
        result = self.execute(query, (time_from, time_to))
        return self.to_json(*result, default=int)
    
    def get_all_gunshots(self):
        """Retrieve all gunshots
        @return (json): a JSON object with the result
        """
        query = "SELECT * FROM Gunshots;"
        result = self.execute(query)
        return self.to_json(*result, default=int)

    # TODO: def get_gunshot_by_radius(self, midpoint_coord, radius):
    
    def get_latest_gunshot_id(self):
        """Get the most recent gunshot ID
        @return (int): the most recent gunshot ID
        """
        query = "SELECT MAX(gunshot_id) AS gunshot_id FROM Gunshots;"
        result = self.execute(query)
        # if len(result[0]) > 0 and not None in result[0]:
        return result[0][0][0] or 0
        # return self.to_json(result, default=int)

    def to_json(self, rows, columns, default = None):
        """Convert database results to a JSON object
        @param rows (list): a list of tuples containing the query result
        @param default (type): the type to cast to by default if unable to determine data type
        @return (json): a JSON object with the query result
        """
        # convert the list of tuples to a list of dictionaries
        row_dicts = []
        for row in rows:
            row_dict = {}
            for i in range(len(row)):
                try:
                    # get the name of the column and set its value in the dictionary
                    row_dict[columns[i]] = row[i]
                except IndexError:
                    pass
            row_dicts.append(row_dict)
        json_str = json.dumps(row_dicts, default=default)

        # remove the wrapping array for single results
        if len(row_dicts) == 1:
            json_str = json_str[1:-1]

        json_obj = json.loads(json_str)
        
        # convert the list of dictionaries to a JSON object
        return json_obj