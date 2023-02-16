from __future__ import annotations #Crazy hack

import geopy.distance
import math
import sys
import os.path
import os
import scipy.optimize
import numpy as np

    #Maximum distance a gunshot can be picked up from
MAX_DISTANCE = 1000 
    #Speed of sound in meters per second
SPEED_OF_SOUND = 343
    #Speed of sound in meters per millisecond
SPEED_OF_SOUND_MS = 343/1000
    #Maximum difference of time in milliseconds between when two different clients pick up a gunshot
MAX_TIME_DIFF = MAX_DISTANCE/SPEED_OF_SOUND_MS


class Position:
    def __init__(self, latitude, longitude, altitude):
        self.v = (latitude, longitude, altitude)

    def __str__(self):
        return self.v.__str__()

    @property
    def spherical(self):
        return self.v[:2]

    @property
    def latitude(self):
        return self.v[0]

    @property
    def longitude(self):
        return self.v[1]

    @property
    def altitude(self):
        return self.v[2]

    def distance(self, pos : Position):
        #Calculate distance in meters from just latitude and longitude
        geodesic = geopy.distance.geodesic(self.spherical, pos.spherical).m
        # Take altitude into account using pythagorean theorem
        # This will not be accurate for very large distances due
        # to earth's curvature but will work for these purposes
        # with (relatively) short distances
        return math.sqrt(geodesic**2 + (self.altitude - pos.altitude)**2)

    def midpoint(positions : list[Position]):
        n = len(positions)
        # Again, this will be accurate for these
        # (relatively) short distances
        return Position(sum(p.latitude for p in positions) / n, 
                        sum(p.longitude for p in positions) / n, 
                        sum(p.altitude for p in positions) / n)

    def tdoa(positions : list[Position], timestamps : list[int]):
        n = len(positions)
        # The objective functions to minimize using Nelder-Mead algorithm
        # It is a summation of errors squared e_0^2 + e_1^2 + ... + e_n^2
        # where error e_i is given by 
        # e_i = distance between position P_x and P_i - distance traveled by sound between timestamp T_x and T_i
        # Where position P_x and T_x are the variables and P_i and T_i are positions and timestamp of
        # gunshot report i
        def objective(x):
            return sum((positions[i].distance(Position(*x[:3])) - SPEED_OF_SOUND_MS*(timestamps[i]-x[3]))**2 for i in range(n))
        # Starting guess P_0 is midpoint of positions and T_0 earliest gunshot report timestamp
        x0 = Position.midpoint(positions).v + (min(timestamps),)
        # Bounds on latitude and longitude
        bounds = ((-90,90),(-180,180),(None,None),(None,None))
        # Minimize using scipy's optimization library
        sol = scipy.optimize.minimize(objective, x0=x0, method="Nelder-Mead", bounds=bounds, tol=10^-4).x

        #print("Minimization solution:",sol,"Objective value:",objective(sol))
        return Position(*sol[:3]),int(sol[3])

class GunshotReport:
    def __init__(self, position : Position, timestamp: int, weapontype: str, clientid: int):
        self.position = position
        self.timestamp = timestamp
        self.weapontype = weapontype
        self.clientid = clientid

    @classmethod
    def from_coordinates(cls, coordinates : tuple[float, float, float], timestamp: int, weapontype: str, clientid: int):
        return cls(Position(*coordinates), timestamp, weapontype, clientid)

    def description(self):
        return ("Gunshot of type: " + self.weapontype + " detected at time: " + str(self.timestamp) + 
            " by client " + str(self.clientid) + " at position: " + str(self.position.latitude) + ", " + 
            str(self.position.longitude) + " at altitude: " + str(self.position.altitude))

class GunshotEvent:
    def __init__(self, gunshot: GunshotReport):
        self.gunshots = [gunshot]
        self.clients = {gunshot.clientid}
        self.weapontype = gunshot.weapontype
        self.timestamp_firstshot = gunshot.timestamp
        self.timestamp_latestshot = gunshot.timestamp

    def try_sameclient(self, gunshot: GunshotReport):
        if (gunshot.clientid in self.clients and
                gunshot.weapontype == self.weapontype and
                self.inside_range(gunshot)):
            self.gunshots.append(gunshot)
            self.timestamp_latestshot = max(self.timestamp_latestshot, gunshot.timestamp)
            self.timestamp_firstshot = min(self.timestamp_firstshot, gunshot.timestamp)
            return True
        return False

    def try_newclient(self, gunshot: GunshotReport):
        if (gunshot.weapontype == self.weapontype and
                self.inside_range(gunshot)):
            self.gunshots.append(gunshot)
            self.clients.add(gunshot.clientid)
            self.timestamp_latestshot = max(self.timestamp_latestshot, gunshot.timestamp)
            self.timestamp_firstshot = min(self.timestamp_firstshot, gunshot.timestamp)
            return True
        return False

    def inside_range(self, gunshot: GunshotReport):
        for gs in self.gunshots:
            if gunshot.position.distance(gs.position) > MAX_DISTANCE*2:
                return False
        return True

    def get_first_reports(self):
        clients = set()
        sorted_gunshots = sorted(self.gunshots, key=lambda gunshot: gunshot.timestamp)
        for gunshot in sorted_gunshots:
            if gunshot.clientid not in clients:
                yield gunshot
                clients.add(gunshot.clientid)

    def approximations(self):
        first_reports = list(self.get_first_reports()) #Only the report of the first gunshot heard, not following gunshots
        if len(first_reports) < 3:
            return None, None
        self.position, self.timestamp = Position.tdoa([r.position for r in first_reports], [r.timestamp for r in first_reports])
        return self.position, self.timestamp

#------------TESTING-----------

def popevent(event):
    gunshotcount = 0
    for client in event.clients:
        gunshotsheard = 0
        for gunshot in event.gunshots:
            if gunshot.clientid == client:
                gunshotsheard += 1
        gunshotcount = max(gunshotcount, gunshotsheard)
    print("\t" + str(gunshotcount) + " " + event.weapontype + " gunshot(s) heard by " + str(len(event.clients)) + " clients at time: " + str(event.timestamp_firstshot))
    if len(event.clients) < 3:
        print("\tPosition not able to be pinpointed due to too few reports")
    else:
        pos, time = event.approximations()
        print("Approximate position: " + str(pos) + " timestamp: " + str(time))

def testfile(gunshotstrings):
    events = []
    for line in gunshotstrings:
        latitude, longitude, altitude, timestamp, weapontype, clientid = line.split(" ")[:6]
        gunshot = GunshotReport.from_coordinates((float(latitude), float(longitude), float(altitude)), int(timestamp), weapontype, int(clientid))
        [popevent(event) for event in events if gunshot.timestamp - event.timestamp_latestshot > MAX_TIME_DIFF]
        events = [event for event in events if gunshot.timestamp - event.timestamp_latestshot <= MAX_TIME_DIFF]
        for event in events:
            if event.try_sameclient(gunshot) == True:
                break
        else: #No fitting event
            for event in events:
                if event.try_newclient(gunshot) == True:
                    break
            else:
                events.append(GunshotEvent(gunshot))
        print(gunshot.description())
    for event in events:
        popevent(event)

#Test system using test file of example gunshots or directory of test files
if __name__ == "__main__":
    if len(sys.argv) <= 1: #No arguments
        print("No test files specified.")
    elif os.path.isfile(sys.argv[1]):
        print("--- " + sys.argv[1])
        testfile(open(sys.argv[1]).readlines())
    elif os.path.isdir(sys.argv[1]):
        for path in os.listdir(sys.argv[1]):
            path = os.path.join(sys.argv[1], path)
            if os.path.isfile(path):
                print("--- " + path)
                testfile(open(path).readlines())
    else:
        print("Invalid file or directory.")
        