# The API (work in progress)

## Introduction
This API allows you to access various resources through a RESTful interface. It requires a valid JSON Web Token (JWT) to authenticate each request. The JWT must be included in the Authorization header of each request.

## Requirements
* Python 3.x
* MariaDB / MySQL
* Packages:
    * libmariadb3 (Debian)
    * mariadb-connector-c (Arch)
* pip packages: Flask, mariadb, PyJWT

## Installation
To install the required packages, run the following commands:
```bash
sudo apt-get install libmariadb3
pip install Flask mariadb PyJWT
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
export MY_API_SECRET_KEY="your_secret_key_here"
```

## Usage
To start the API server, run the following command:
```bash
python -m flask --app app run
```

## Endpoints
* **`GET  /register`** - Retrieve a JWT token used to authorize API calls.
* **`POST /api/guns`** - Add a gun to the database
* **`GET  /api/guns`** - Search for a gun in the database
* **`POST /api/reports`** - Add a report to the database
* **`GET  /api/reports`** - Search for a report in the database
* **`POST /api/gunshots`** - Add record of a determined gunshot based on reports
* **`GET  /api/gunshots`** - Search for gunshots based on time or location (or both)