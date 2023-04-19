from __future__ import annotations  # Crazy hack

import geopy.distance
import math
import sys
import os.path
import os
import scipy.optimize

MAX_DISTANCE = 1000
"""Maximum distance in meters a gunshot can be picked up from"""
SPEED_OF_SOUND_MS = 343/1000
"""Speed of sound in meters per millisecond"""
MAX_TIME_DIFF = MAX_DISTANCE/SPEED_OF_SOUND_MS
"""Maximum difference of time in milliseconds between when two different clients pick up a gunshot"""


class Position:
    def __init__(self, latitude, longitude, altitude):
        """
        @param latitude: latitude in degrees between -90 and 90
        @type latitude: float
        @param longitude: longitude in degrees between -180 and 180
        @type longitude: float
        @param altitude: altitude in meters above sea level
        @type altitude: float
        """
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

    def distance(self, position: Position) -> float:
        """
        Returns the distance in meters from calling position object to
        given position object

        @param position: Position from which to calculate distance to
        @return: Distance in meters to specified position
        """

        # Calculate distance in meters from just latitude and longitude
        geodesic = geopy.distance.geodesic(self.spherical, position.spherical).m
        # Take altitude into account using pythagorean theorem
        # This will not be accurate for very large distances due
        # to earth's curvature but will work for these purposes
        # with (relatively) short distances
        return math.sqrt(geodesic**2 + (self.altitude - position.altitude)**2)
    
    def shift(self, magnitude : float, theta: float, phi: float):
        new_altitude = self.altitude + math.sin(phi) * magnitude
        new_geodesic = geopy.distance.distance(meters=magnitude * math.cos(phi)).destination(
            self.v[:2], bearing=theta)
        return Position(new_geodesic[0], new_geodesic[1], new_altitude)

    def midpoint(positions: list[Position]) -> Position:
        """
        Returns the midpoint of a list of positions

        @param position: List of positions to find midpoint of
        @return: Position in center of all given positions
        """
        n = len(positions)
        # Again, this will be accurate for these
        # (relatively) short distances
        return Position(sum(p.latitude for p in positions) / n,
                        sum(p.longitude for p in positions) / n,
                        sum(p.altitude for p in positions) / n)

    def tdoa(positions: list[Position], timestamps: list[int]) -> tuple[Position, int]:
        """
        Given a list of positions of sound recievers and when these
        recievers picked up a sound, it uses TDOA
        (Time Difference of Arrival) to estimate the positions of
        the sound source and when it sent out the sound signal. 
        Given that the positions and timestamps of the receivers
        are subject to noise, the problem is set up as an optimization
        problem, where we try to find the sound source position and
        timestamp that reduces the error for each sound receiver. 
        Error is defined for a sound receiver as the difference
        between the distance to sound sound and distance traveled
        by sound.

        @param positions: List of positions of sound recievers
        @param timestamps: List of timestamps in milliseconds when
        the sound recievers 'heard' the sound
        @return: Estimated position and timestamp of sound source
        """
        n = len(positions)

       

        def error(x, i):
            return d(x, i) - SPEED_OF_SOUND_MS*(timestamps[i]-x[3])
        
        def d(x, i):
            return positions[i].distance(Position(*x[:3]))
        
        # The objective functions to minimize using Nelder-Mead algorithm
        # It is a summation of errors squared e_0^2 + e_1^2 + ... + e_n^2
        # where error e_i is given by
        # e_i = distance between position P_x and P_i - distance traveled by sound between timestamp T_x and T_i
        # Where position P_x and T_x are the variables and P_i and T_i are positions and timestamp of
        # gunshot report i
        # If number of positions n == 3 then we will also try to minimize distance between
        # transmission and and receiver since position can only be determined on a line
        # in 3d space
        def objective(x):
            return sum((d(x, i) * 0.001 if n == 3 else 0) + error(x, i)**2 for i in range(n))
        # Starting guess P_0 is midpoint of positions and T_0 earliest gunshot report timestamp
        x0 = Position.midpoint(positions).v + (min(timestamps),)
        # Bounds on latitude and longitude
        bounds = ((-90, 90), (-180, 180), (None, None), (None, None))
        # Minimize using scipy's optimization library
        sol = scipy.optimize.minimize(objective, x0=x0, method="Nelder-Mead", bounds=bounds, tol=10^-4).x

        # print("Minimization solution:",sol,"Objective value:",objective(sol))

        if objective(sol) > 10000 or any(d(sol, i) > MAX_DISTANCE for i in range(n)):
            return None, None
        return Position(*sol[:3]), int(sol[3])

class GunshotReport:
    def __init__(self, position: Position, timestamp: int, weapontype: str, clientid: str):
        """
        @param position: Position of the client that heard the gunshot
        @param timestamp: Timestamps in milliseconds when gunshot was
        detected by client
        @param weapontype: Type of weapon used as estimated by the client
        @param clientid: Unique client id to differentiate clients
        """
        self.position = position
        self.timestamp = timestamp
        self.weapontype = weapontype
        self.clientid = clientid

    @classmethod
    def from_coordinates(cls, coordinates: tuple[float, float, float], timestamp: int, weapontype: str, clientid: str):
        """
        @param coordinates: Tuple of latitude, longitude, and altitude.
        Latitude and longitude is given in degress, altitude in meters
        @param timestamp: Timestamps in milliseconds when gunshot was
        detected by client
        @param weapontype: Type of weapon used as estimated by the client
        @param clientid: Unique client id to differentiate clients
        """
        return cls(Position(*coordinates), timestamp, weapontype, clientid)

    def __str__(self):
        """
        Returns a string describing the gunshot report
        """
        return ("Gunshot of type: " + self.weapontype + " detected at time: " + str(self.timestamp) +
                " by client " + self.clientid + " at position: " + str(self.position.latitude) + ", " +
                str(self.position.longitude) + " at altitude: " + str(self.position.altitude))

    def __repr__(self):
        return str(self)

class GunshotEvent:
    MIN_CLIENTS = 3

    def __init__(self, gunshot: GunshotReport):
        """
        @param gunshot: The first gunshot report of a new event,
        used to group together with other gunshot reports that are
        believed to be related to the same shooting
        """
        self.gunshots = [gunshot]
        self.clients = {gunshot.clientid}
        self.weapontype = gunshot.weapontype
        self.timestamp_first_report = gunshot.timestamp
        self.timestamp_latest_report = gunshot.timestamp

    def try_sameclient(self, gunshot: GunshotReport) -> bool:
        """
        Check if the client that reported given gunshot report,
        has previously reported gunshots in this event, and
        the gunshot report seems to be related to this event.
        If so, it adds it to this event and returns True, 
        otherwise it returns False.

        @param gunshot: The gunshot report to check if it belongs
        @return: True if gunshot report was added to event,
        otherwise False.
        """
        if (gunshot.clientid in self.clients and
                gunshot.weapontype == self.weapontype and
                self._inside_range(gunshot)):
            self.gunshots.append(gunshot)
            self.timestamp_latest_report = max(self.timestamp_latest_report, gunshot.timestamp)
            self.timestamp_first_report = min(self.timestamp_first_report, gunshot.timestamp)
            return True
        return False

    def try_newclient(self, gunshot: GunshotReport) -> bool:
        """
        Check if the client that reported given gunshot report,
        has no previously reported gunshots in this event, and
        the gunshot report seems to be related to this event.
        If so, it adds it to this event and returns True, 
        otherwise it returns False.

        @param gunshot: The gunshot report to check if it belongs
        @return: True if gunshot report was added to event,
        otherwise False.
        """
        if (gunshot.clientid not in self.clients and 
                gunshot.weapontype == self.weapontype and
                self._inside_range(gunshot)):
            self.gunshots.append(gunshot)
            self.clients.add(gunshot.clientid)
            self.timestamp_latest_report = max(self.timestamp_latest_report, gunshot.timestamp)
            self.timestamp_first_report = min(self.timestamp_first_report, gunshot.timestamp)
            return True
        return False

    def _inside_range(self, gunshot: GunshotReport):
        """
        Return true if the position of the client reporting a gunshot,
        is close to all other positions of gunshot reports in this
        event. Otherwise returns false.

        @param gunshot: The gunshot report to check
        @return: True if gunshot report was in event range, otherwise
        False
        """
        for gs in self.gunshots:
            if gunshot.position.distance(gs.position) > MAX_DISTANCE*2:
                return False
        return True

    def _get_first_reports(self) -> iter[Position]:
        """
        Return an iterable of only the gunshot reports corresponding
        to the first gunshot fired in this gunfire event and not the
        following gunshots fired by the same subject.

        @return: Iterable of gunshot reports of the first gunshot
        in this event
        """
        clients = set()
        sorted_gunshots = sorted(self.gunshots, key=lambda gunshot: gunshot.timestamp)
        for gunshot in sorted_gunshots:
            if gunshot.clientid not in clients:
                yield gunshot
                clients.add(gunshot.clientid)

    def total_firings(self) -> int:
        """
        Estimates the amount of gunshots fired based upon amount of report by individual clients

        @return: Integer of amount of gunshots fired
        """
        return max(len(None for report in self.reports if report.clientid == clientid) for clientid in self.clients)
    
    def approximations(self) -> tuple[Position, int]:
        """
        Tries to estimate the position where the gun was fired and
        what time instance the first shot was fired using TDOA.
        If unable to make an estimation, returns (None,None)

        @return: Tuple of position, timestamp if estimate was
        possible, otherwise None,None
        """
        first_reports = list(self._get_first_reports()) # Only the report of the first gunshot heard, not following gunshots
        if len(first_reports) < 3:
            self.position, self.timestamp = None, None
        else:
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
    print("\t" + str(gunshotcount) + " " + event.weapontype + " gunshot(s) heard by " +
          str(len(event.clients)) + " clients at time: " + str(event.timestamp_first_report))
    if len(event.clients) < 3:
        print("\tPosition not able to be pinpointed due to too few reports")
    else:
        pos, time = event.approximations()
        print("Approximate position: " + str(pos) + " timestamp: " + str(time))

def testfile(gunshotstrings):
    events = []
    for line in gunshotstrings:
        latitude, longitude, altitude, timestamp, weapontype, clientid = line.split(" ")[:6]
        gunshot = GunshotReport.from_coordinates((float(latitude), float(longitude), float(altitude)), int(timestamp), weapontype, clientid)
        [popevent(event) for event in events if gunshot.timestamp - event.timestamp_latest_report > MAX_TIME_DIFF]
        events = [event for event in events if gunshot.timestamp - event.timestamp_latest_report <= MAX_TIME_DIFF]
        for event in events:
            if event.try_sameclient(gunshot) == True:
                break
        else:  # No fitting event
            for event in events:
                if event.try_newclient(gunshot) == True:
                    break
            else:
                events.append(GunshotEvent(gunshot))
        print(gunshot)
    for event in events:
        popevent(event)

# Test system using test file of example gunshots or directory of test files
if __name__ == "__main__":
    if len(sys.argv) <= 1: # No arguments
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
