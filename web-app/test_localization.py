from gunshot import Position, SPEED_OF_SOUND_MS
import requests, time, random, geopy.distance

url = "http://lukas.tottes.net"
    
class Test:
    def __init__(self, start_timestamp, max_timestamp_error = 100, max_position_error = 15, client_amount = (3,8),
                 client_distance = (0,500), client_heard = 0.9):
        self.start_timestamp = start_timestamp
        self.gunshots_fired = 0
        self.max_timestamp_error = max_timestamp_error
        self.max_position_error = max_position_error
        self.client_amount = client_amount
        self.client_distance = client_distance
        self.client_heard = client_heard

        self.gunshot_lat, self.gunshot_long = random.uniform(54, 69), random.uniform(10, 26)
        self.gunshot_pos = Position(self.gunshot_lat, self.gunshot_long, 0)

        self.clients = [Client(self) for _ in range(random.randint(*self.client_amount))]

    def run(self, gunshot_amount = 1):
        for i in range(gunshot_amount):
            for client in self.clients:
                client.sound_wave(self.start_timestamp + self.gunshots_fired * 2 * self.max_timestamp_error)
            self.gunshots_fired += 1
        
    def collect_results(self):
        headers = {
            "Authorization": self.token
        }
        json = {
            "time_from": self.start_timestamp - 5000,
            "time_to": self.start_timestamp + 5000
        }
        resp = requests.get(url + "/api/gunshots", params=json, headers=headers)
        if not (resp.status_code >= 200 and resp.status_code < 300):
            raise Exception(resp)
        
        events = resp.json()
        if len(events) == 0:
            print("Could not find event")
        elif len(events) > 1:
            print("Too many events")
        else:
            event = events[0]
            event_pos = Position(event["coord_lat"], event["coord_long"], event["coord_alt"])
            print(f"Error in meters: {self.gunshot_pos.distance(event_pos)}")
            if self.gunshots_fired != event["shots_fired"]:
                print(f"Shots fired: {self.gunshots_fired}, heard: {event['shots_fired']}")

class Client:
    def __init__(self, test : Test):
        self.test = test
        self.first_report = True

        self.true_pos = test.gunshot_pos.shift(random.uniform(*test.client_distance), 
                                                               theta=random.uniform(0,360),
                                                               phi=random.uniform(-10,10))
        self.gps_pos = self.true_pos.shift(random.uniform(0,test.max_position_error), 
                                                               theta=random.uniform(0,360),
                                                               phi=random.uniform(-10,10))
        
        self.time_to_arrival = int(test.gunshot_pos.distance(self.true_pos) * SPEED_OF_SOUND_MS)

        self.token = Client.get_token()

    def sound_wave(self, gunshot_timestamp):
        if self.first_report or random.uniform(0,1) <= self.test.client_heard:
            self.first_report = False
            self.send_report(gunshot_timestamp + self.time_to_arrival + random.randrange(0,self.test.max_timestamp_error))
            input()

    def get_token():
        data = requests.get(url + "/register")
        return data.json()["token"]
    
    def send_report(self, timestamp):
        headers = {
            "Authorization": self.token
        }
        json = {
            "timestamp": timestamp,
            "coord_lat": self.gps_pos.latitude,
            "coord_long": self.gps_pos.longitude,
            "coord_alt": self.gps_pos.altitude,
            "gun": "AR-15"
        }
        resp = requests.post(url + "/api/reports", json=json, headers=headers)
        if not (resp.status_code >= 200 and resp.status_code < 300):
            raise Exception(resp.text)

if __name__ == "__main__":
    test = Test(time.time() * 1000)
    test.run(1)
    test.collect_results()
