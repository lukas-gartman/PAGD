# The API (work in progress)

## Introduction
This API allows you to access various resources through a RESTful interface. It requires a valid JSON Web Token (JWT) to authenticate each request. The JWT must be included in the Authorization header of each request.

## Requirements
* Python 3.x
* MariaDB / MySQL
* Packages:
    * libmysqlclient-dev (Debian)
    * mysql-connector-c (Arch)
* pip packages: flask PyJwt mysql-connector-python geopy scipy firebase-admin

## Installation
To install the required packages, run the following commands:
```bash
sudo apt-get install libmysqlclient-dev
pip install flask PyJwt mysql-connector-python geopy scipy firebase_admin
```

## Configuration
### Generating a secret key
To generate a secure secret key for use with JWT, you can use the **`secrets`** module in Python. Here is an example of how to generate a 256-bit secret key:
```python
import secrets
secret_key = secrets.token_urlsafe(32)
```

### Storing the secret key in environment variables on Linux
To store the secret key in an environment variable on Linux, you can add the following line to your **`.bashrc`** or **`.bash_profile`** file:
```bash
export JWT_SECRET_KEY="your_secret_key_here"
```

### Retrieving Firebase credentials
This project uses Firebase Cloud Messaging for event based communication between the server and the app. Check out [this guide](https://firebaseopensource.com/projects/firebase/quickstart-js/messaging/readme/) for more information on how to get started.

Download your credentials from the Firebase console and store its file path in environment by adding the following line to your **`.bashrc`** or **`.bash_profile`** file:
```bash
export FIREBASE_CREDENTIALS="/path/to/credentials.json"
```

## Usage
To start the API server, run the following command:
```bash
python main.py
```

## Endpoints
* **`GET  /register`** - Retrieve a JWT token used to authorize API calls.
* **`POST /api/guns`** - Add a gun to the database
    * gun_name (string): the name of the gun
    * gun_type (string): the type of gun
* **`GET  /api/guns`** - Search for a gun in the database or return all guns
    * gun_name (string, optional): the name of the gun
* **`POST /api/reports`** - Add a report to the database
    * timestamp (int): the UNIX timestamp of the report
    * coord_lat (float): the latitude coordinate
    * coord_long (float): the longitude coordinate
    * coord_alt (float): the altitude coordinate
    * gun (string): the name of the gun
* **`GET  /api/reports`** - Search for a report in the database
    * report_id (int, optional): the report ID
    * time_from (int, optional): UNIX timestamp of the start of the range
    * time_to (int, optional): UNIX timestamp of the beginning of the range
* **`POST /api/gunshots`** - Add record of a determined gunshot based on the given report
    * gunshot_id (int): the gunshot ID
    * report_id (int): the report ID which the determined gunshot is based on
    * timestamp (int): determined UNIX timestamp of the gunshot
    * coord_lat (float): the latitude coordinate
    * coord_long (float): the longitude coordinate
    * coord_alt (float): the altitude coordinate
    * gun (string): the name of the gun
    * shots_fired (int): the number of shots that were fired in this event
* **`PUT  /api/gunshots`** - Update the data of a gunshot with the given ID
    * gunshot_id (int): the gunshot ID
    * timestamp (int): determined UNIX timestamp of the gunshot
    * coord_lat (float): the latitude coordinate
    * coord_long (float): the longitude coordinate
    * coord_alt (float): the altitude coordinate
    * shots_fired (int): the number of shots that were fired in this event
* **`GET  /api/gunshots`** - Search for gunshots based on time or location (or both)
    * gunshot_id (int, optional): the gunshot ID
    * time_from (int, optional): UNIX timestamp of the start of the range
    * time_to (int, optional): UNIX timestamp of the beginning of the range
* **`GET  /api/gunshots/latest`** - Get the most recent gunshot ID

## Usage
Example app for making requests to the API
```python
import requests

# Retrieve a token. Note: this should be stored somewhere, not retrieved for every run.
token = requests.get("http://<hostname>/register").json()["token"]
headers = {"Authorization": token}
url = "http://<hostname>/api/gunshots"

# Add a temporary gunshot entry
requests.post(url, headers=headers, json={"gunshot_id": 1, "gun": "AR-15", "report": 1})
# >> 
# Update the gunshot entry with its information
requests.put(url, headers=headers, json={"gunshot_id": 1, "coord_lat": 2.0, "coord_long": 3.0, "coord_alt": 4.0, "timestamp": 1499405054287, "gun": "AR-15", "shots_fired": 1})
```
