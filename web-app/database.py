import mariadb
import json

class Database:
    def __init__(self, host, port, user, password, database):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database
        self.conn = None
        self.cursor = None
        self._connect()

    def __del__(self):
        self.close()

    def __enter__(self):
        return self

    def __exit__(self):
        self.close()

    def _connect(self):
        self.conn = mariadb.connect(
            host = self.host,
            port = self.port,
            user = self.user,
            password = self.password,
            database = self.database)
        self.cursor = self.conn.cursor()

    """Execute an SQL query
    @param query string: the SQL query to be executed. Use %s for parameters
    @param values tuple: the parameter values for each %s in the query
    @return 
    """
    def execute(self, query, values = None):
        if self.cursor is None:
            self._connect()

        self.cursor.execute(query, values)
        result = self.cursor.fetchall()
        self.conn.commit()

        return result

    def close(self):
        if self.cursor is not None:
            self.cursor.close()
        if self.conn is not None:
            self.conn.close()
    
    def to_json(self, rows, default = None):
        # convert the list of tuples to a list of dictionaries
        row_dicts = []
        for row in rows:
            row_dict = {}
            for i in range(len(row)):
                row_dict[self.cursor.description[i][0]] = row[i]
            row_dicts.append(row_dict)
        json_obj = json.dumps(row_dicts, default=default)

        if len(row_dicts) == 1:
            json_obj = json_obj[1:-1]
        
        # convert the list of dictionaries to a JSON object
        return json_obj