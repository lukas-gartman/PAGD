import mysql.connector.pooling
import time

class Database:
    def __init__(self, host, port, user, password, database, pool_size = 32):
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database
        self.pool = self._create_pool(pool_size)
    
    def _create_pool(self, pool_size):
        pool = mysql.connector.pooling.MySQLConnectionPool(
            pool_name = "pagd_pool",
            pool_size = pool_size,
            pool_reset_session = True,
            host = self.host,
            port = self.port,
            user = self.user,
            password = self.password,
            database = self.database)
        return pool

    def _get_connection(self, sleep_timer = 0):
        try:
            conn = self.pool.get_connection()
        except mysql.connector.errors.PoolError:
            if sleep_timer > 10:
                return None
            sleep_timer += 1
            print(f"Failed getting connection; pool exhausted. Trying again in {sleep_timer}s...")
            time.sleep(sleep_timer)
            conn = self._get_connection(sleep_timer)
        if conn is None:
            print(f"Failed to get a connection after {sleep_timer} tries. Giving up...")
        return conn

    def execute(self, query, values = None):
        """Execute an SQL query
        @param query (string): the SQL query to be executed. Use %s for parameters
        @param values (tuple, optional): the parameter values for each %s in the query
        @return list: the query result
        """
        conn = self._get_connection()
        if conn is None:
            return ([], [])
        cursor = conn.cursor()

        try:
            if type(values) is list: # Execute query in bulk
                cursor.executemany(query, values)
            else:
                cursor.execute(query, values)
                
            desc = cursor.description
            if desc is not None: # Checks if the query returned something
                result = cursor.fetchall()
            else:
                result = []
            
            conn.commit()
        except Exception as e:
            conn.rollback() # Roll back to the previous state in case of error
            raise e
        finally:
            cursor.close()
            conn.close()  # release the connection back to the pool

        columns = self._extract_columns(desc)
        return (result, columns)
    
    def _extract_columns(self, description):
        columns = []
        for desc in description:
            columns.append(desc[0])
        return columns
    
    def execute_transaction(self, queries, values = None):
        """Execute multiple SQL queries as a transaction
        @param queries (list[string]): the SQL queries to be executed
        @param values (list[tuple], optional): the parameters in order of queries
        @return list[list]: the query result
        """
        conn = self._get_connection()
        if conn is None:
            return ([], [])
        cursor = conn.cursor()

        try:
            result = []
            for q, v in zip(queries, values):
                try:
                    cursor.execute(q, v)
                    desc = cursor.description
                    if desc is not None:
                        result.append(cursor.fetchall())
                    else:
                        result.append([])
                except Exception as e:
                    print("Error occurred, rolling back database...", str(e), sep="\n\t")
                    self.conn.rollback()
                    return []

            conn.commit()
        except Exception as e:
            conn.rollback() # Roll back to the previous state in case of error
            raise e
        finally:
            cursor.close()
            conn.close()  # release the connection back to the pool

        columns = self._extract_columns(desc)
        return (result, columns)
