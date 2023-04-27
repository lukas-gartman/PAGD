from getpass import getpass
from flask import Flask
from app import create_routes

from pagdDB import PagdDB
from gunshot_subject import GunshotSubject
from gunshot_observer import GunshotObserver

def main():
    # Database
    db = PagdDB("localhost", "pagd", getpass("Database password: "))

    # API server
    app = Flask(__name__)

    # Watch the API server for updates
    gunshot_subject = GunshotSubject()
    gunshot_observer = GunshotObserver(gunshot_subject, db)

    # Set up the API server routes
    create_routes(app, db, gunshot_subject)
    app.run(debug=False, threaded=True)

    
if __name__ == "__main__":
    main()
