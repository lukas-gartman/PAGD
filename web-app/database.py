import mariadb

class Database:
    def __init__(self, host, port, user, password, database):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database
        self.conn = None
        self.cursor = None
        self.connect()

    def __del__(self):
        self.close()

    def __enter__(self):
        return self

    def __exit__(self):
        self.close()

    def connect(self):
        self.conn = mariadb.connect(
            host = self.host,
            port = self.port,
            user = self.user,
            password = self.password,
            database=self.database)
        self.cursor = self.conn.cursor()

    def execute(self, query, values = None):
        if self.cursor is None:
            self.connect()

        self.cursor.execute(query, values)
        result = self.cursor.fetchall()
        self.conn.commit()
        
        return result

    def close(self):
        if self.cursor is not None:
            self.cursor.close()
        if self.conn is not None:
            self.conn.close()