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
        self._close()

    def __enter__(self):
        return self

    def __exit__(self):
        self._close()

    def _connect(self):
        self.conn = mariadb.connect(
            host = self.host,
            port = self.port,
            user = self.user,
            password = self.password,
            database = self.database)
        self.cursor = self.conn.cursor()
    
    def _close(self):
        if self.cursor is not None:
            self.cursor.close()
        if self.conn is not None:
            self.conn.close()
    
    def _is_connected(self):
        try:
            self.conn.ping()
        except:
            return False
        return True

    def execute(self, query, values = None):
        """Execute an SQL query
        @param query (string): the SQL query to be executed. Use %s for parameters
        @param values (tuple, optional): the parameter values for each %s in the query
        @return list: the query result
        """
        if not self._is_connected():
            print("MySQL server has gone away. Reconnecting...")
            self._connect()
        
        self.cursor.execute(query, values)
        if self.cursor.description is not None:
            result = self.cursor.fetchall()
        else:
            result = []
        self.conn.commit()

        return result
    
    def execute_transaction(self, queries, values = None):
        """Execute multiple SQL queries as a transaction
        @param queries (list[string]): the SQL queries to be executed
        @param values (list[tuple], optional): the parameters in order of queries
        @return list[list]: the query result
        """
        if not self._is_connected():
            print("MySQL server has gone away. Reconnecting...")
            self._connect()
        
        result = []
        for q, v in zip(queries, values):
            try:
                self.cursor.execute(q, v)
                if self.cursor.description is not None:
                    result.append(self.cursor.fetchall())
                else:
                    result.append([])
            except Exception as e:
                print("Error occurred, rolling back database...", str(e), sep="\n\t")
                self.conn.rollback()
                return []

        self.conn.commit()

        return result
    
    def to_json(self, rows, default = None):
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
                    row_dict[self.cursor.description[i][0]] = row[i]
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