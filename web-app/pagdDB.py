from database import Database

class PagdDB(Database):
    def __init__(self, user, password):
        super().__init__("tottes.net", 3306, user, password, "pagd")

    def add_gun(self, gun_name, gun_type):
        query = "INSERT INTO Guns VALUES (%s, %s) RETURNING *;"
        try:
            return self.execute(query, (gun_name, gun_type))
        except Exception:
            return False

    def get_gun(self, gun_name):
        query = "SELECT * FROM Guns WHERE name = %s;"
        return self.execute(query, (gun_name,))

    def get_all_guns(self):
        query = "SELECT * FROM Guns;"
        return self.execute(query)

    def add_report(self, timestamp, coord_lat, coord_long, gun):
        coord = f"{coord_lat},{coord_long}"
        query = "INSERT INTO Reports (timestamp, coord, gun) VALUES (FROM_UNIXTIME(%s), %s, %s) RETURNING *;"
        return self.execute(query, (timestamp, coord, gun))

    def get_report(self, report_id):
        query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Reports WHERE report_id = %s;"
        return self.execute(query, (report_id,))
    
    def get_report_range(self, time_from, time_to):
        query = "SELECT report_id, (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Reports WHERE timestamp >= FROM_UNIXTIME(%s) AND timestamp < FROM_UNIXTIME(%s);"
        return self.execute(query, (time_from / 1000, time_to / 1000))

    def add_gunshot(self, timestamp, coord_lat, coord_long, gun):
        coord = f"{coord_lat},{coord_long}"
        query = "INSERT INTO Gunshots (timestamp, coord, gun) VALUES (FROM_UNIXTIME(%s), %s, %s) RETURNING *;"
        return self.execute(query, (timestamp / 1000, coord, gun))
    
    def get_gunshot(self, timestamp = None, coord = None):
        result = []

        if timestamp and coord:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots WHERE timestamp = FROM_UNIXTIME(%s) AND coord = %s;"
            result = self.execute(query, (timestamp / 1000, coord))
        elif timestamp:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots WHERE timestamp = FROM_UNIXTIME(%s);"
            result = self.execute(query, (timestamp / 1000,))
        elif coord:
            query = "SELECT (SELECT ROUND(UNIX_TIMESTAMP(timestamp) * 1000)) AS timestamp, coord, gun FROM Gunshots WHERE coord = %s;"
            result = self.execute(query, (coord,))
        
        return result