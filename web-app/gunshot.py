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
            return sum(error(x, i)**2 for i in range(n))
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

    def fits(self, report: GunshotReport):
        '''
        Returns true if gunshot report is related to this event.
        Functions based on a set of rules as follows:
        - position of report is within range of other reports in event
        - timestamp of report is in within range of other reports

        @param report: The gunshot report to check if it belongs
        @return: True if gunshot report is related to this event,
        otherwise False
        '''
        return (report.weapontype == self.weapontype and
                self._inside_range(report) and
                self._within_time_margin(report))

    def client_has_added(self, report: GunshotReport):
        '''
        Return true if the client that reported given gunshot report,
        has previously reported gunshots in this event

        @param report: The gunshot report to check for client match
        @return: True if client reporting has previosly reported to
        this event, otherwise False
        '''
        return report.clientid in self.clients
    
    def add_report(self, report: GunshotReport):
        '''
        Add report to event

        @param report: The gunshot report to add to event
        '''
        self.gunshots.append(report)
        self.clients.add(report.clientid)

    def _inside_range(self, report: GunshotReport):
        """
        Return True if the position of the client reporting a gunshot
        is close to all other positions of gunshot reports in this
        event, otherwise returns false.

        @param report: The gunshot report to check
        @return: True if gunshot report was in event range, otherwise
        False
        """
        return all(report.position.distance(gs.position) < MAX_DISTANCE*2 for gs in self.gunshots)
    
    def _within_time_margin(self, report: GunshotReport):
        """
        Return True if the timestamp of gunshot report is close in time
        to any other gunshot report in this event, otherwise returns false.

        @param report: The gunshot report to check
        @return: True if gunshot report was within time margin otherwise
        returns False
        """
        return any(abs(report.timestamp - gs.timestamp) < MAX_TIME_DIFF for gs in self.gunshots)

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
        return max(sum(1 for report in self.gunshots if report.clientid == clientid) for clientid in self.clients)
    
    def approximations(self) -> tuple[Position, int]:
        """
        Tries to estimate the position where the gun was fired and
        what time instance the first shot was fired using TDOA.
        If unable to make an estimation, returns (None,None)

        @return: Tuple of position, timestamp if estimate was
        possible, otherwise None,None
        """
        first_reports = list(self._get_first_reports()) # Only the report of the first gunshot heard, not following gunshots
        if len(first_reports) < 4:
            self.position, self.timestamp = None, None
        else:
            self.position, self.timestamp = Position.tdoa([r.position for r in first_reports], [r.timestamp for r in first_reports])
        return self.position, self.timestamp