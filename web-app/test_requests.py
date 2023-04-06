import requests

# token = requests.get("http://localhost:5000/register").json()["token"]
token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyZWdpc3RyYXRpb24iOjE2Nzk0NDQ5NTQxOTB9.95mAfwlGoMnwcw-zbUnn_l29RiMVjnqrPApnZRLgwLA"
url = "http://localhost:5000/api/reports"
headers = {"Authorization": token}

response = requests.post(url, headers=headers, json={"coord_lat": 2.0, "coord_long": 3.0, "coord_alt": 4.0, "timestamp": 1499405054287, "gun": "AR-15", "report": 3})
response = requests.get(url, headers=headers)
json = response.json()
print(json)