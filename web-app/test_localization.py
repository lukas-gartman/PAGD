from gunshot import Position, GunshotEvent, GunshotReport, SPEED_OF_SOUND_MS
import requests, time, random, geopy.distance

url = "http://lukas.tottes.net"
    
class Test:
    def __init__(self, start_timestamp, max_timestamp_error = 100, max_position_error = 15, client_amount = (8,8),
                 client_distance = (50,500), client_heard = 0.9, local = False):
        self.start_timestamp = start_timestamp
        self.gunshots_fired = 0
        self.max_timestamp_error = max_timestamp_error
        self.max_position_error = max_position_error
        self.client_amount = client_amount
        self.client_distance = client_distance
        self.client_heard = client_heard
        self.local = local

        self.gunshot_lat, self.gunshot_long = random.uniform(54, 69), random.uniform(10, 26)
        self.gunshot_pos = Position(self.gunshot_lat, self.gunshot_long, 0)

        self.clients = [Client(self) for _ in range(random.randint(*self.client_amount))]

        if not local:
            self.token = Client.get_token()

    def run(self, gunshot_amount = 1):
        for i in range(gunshot_amount):
            for client in self.clients:
                client.sound_wave(self.start_timestamp + self.gunshots_fired * 2 * self.max_timestamp_error)
            self.gunshots_fired += 1
        
    def collect_results(self):
        if self.local:
            return self.collect_results_local()

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
        
        event = resp.json()
        print(event)
        if len(event) == 0:
            print("Could not find event")
            return None
        elif "nånting" in event.keys():
            print("Too many events")
            return None
        else:
            event_pos = Position(event["coord_lat"], event["coord_long"], event["coord_alt"])
            print(f"Error in meters: {self.gunshot_pos.distance(event_pos)}")
            return self.gunshot_pos.distance(event_pos)
        
    def collect_results_local(self):
        pos, timestamp = self.event.approximations()
        if pos is None:
            print("Could not determine position")
            return None
        else:
            print(f"Error in meters: {self.gunshot_pos.distance(pos)}")
            return self.gunshot_pos.distance(pos)

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
        
        self.time_to_arrival = int(test.gunshot_pos.distance(self.true_pos) / SPEED_OF_SOUND_MS)

        if not test.local:
            self.token = Client.get_token()
        else:
            self.id = random.randint(0,100000000000)

    def sound_wave(self, gunshot_timestamp):
        if self.first_report or random.uniform(0,1) <= self.test.client_heard:
            self.first_report = False
            if not self.test.local:
                self.send_report(gunshot_timestamp + self.time_to_arrival + random.randrange(0,self.test.max_timestamp_error))
            else:
                self.send_report_local(gunshot_timestamp + self.time_to_arrival + random.randrange(0,self.test.max_timestamp_error))

    def get_token():
        data = requests.get(url + "/register")
        return data.json()["token"]
    
    def send_report(self, report_timestamp):
        headers = {
            "Authorization": self.token
        }
        json = {
            "timestamp": report_timestamp,
            "coord_lat": self.gps_pos.latitude,
            "coord_long": self.gps_pos.longitude,
            "coord_alt": self.gps_pos.altitude,
            "gun": "AR-15"
        }
        resp = requests.post(url + "/api/reports", json=json, headers=headers)
        if not (resp.status_code >= 200 and resp.status_code < 300):
            raise Exception(resp.text)
        
    def send_report_local(self, report_timestamp):
        report = GunshotReport(self.gps_pos, report_timestamp, "AR-15", self.id)
        if not hasattr(self.test, "event"):
            self.test.event = GunshotEvent(report)
        else:
            if self.test.event.fits(report):
                self.test.event.add_report(report)
            else:
                print("Report does not fit in event")

if __name__ == "__main__":
    while True:
        test = Test(int(time.time() * 1000), local=False)
        test.run(1)
        test.collect_results()
