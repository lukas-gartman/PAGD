from flask import Flask

app = Flask(__name__)

@app.route("/")
def hello_world():
    return {
        "message": "Hello, World!"
    }

@app.route("/<name>")
def hello_name(name):
    return {
        "message": f"Hello, {name}!"
    }