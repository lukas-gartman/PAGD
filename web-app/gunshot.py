import geopy.distance
import math
import sys
import os.path
import os
import scipy.optimize
import numpy as np
import itertools

MAXDISTANCE = 1000 #meters
MAXTIME = 3000 #Maximum time in milliseconds between different clients hearing a sound
SPEED_OF_SOUND = 343
SPEED_OF_SOUND_MS = 343/1000
Pos = tuple[float, float, float]

class GunshotReport:
    def __init__(self, position: Pos, timestamp: int, weapontype: str, clientid: int):
        self.position = position
        self.timestamp = timestamp
        self.weapontype = weapontype
        self.clientid = clientid

    def description(self):
        return ("Gunshot of type: " + self.weapontype + " detected at time: " + str(self.timestamp) + 
            " by client " + str(self.clientid) + " at position: " + str(self.position[0]) + ", " + 
            str(self.position[1]) + " at altitude: " + str(self.position[2]))

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
            self.inside_range(gunshot)): #Should always be true since same client is sending
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
            if CoordinateCalculator.distance(gunshot.position, gs.position) > MAXDISTANCE*2:
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
        if len(self.gunshots) < 3:
            return None, None
        first_reports = list(self.get_first_reports())[:10]
        ref = first_reports[0]
        cart_reports = [(CoordinateCalculator.to_cart(ref.position, r.position),r.timestamp) for r in first_reports]
        print(cart_reports)
        combs = itertools.combinations(cart_reports, 3)
        self.position = CoordinateCalculator.from_cart(ref.position,
                        CoordinateCalculator.midpoint([
                        CoordinateCalculator.tdoa(*comb) for comb in combs]))
        self.timestamp = ref.timestamp - int(CoordinateCalculator.distance(ref.position, self.position) * 1000 / SPEED_OF_SOUND)
        return self.position, self.timestamp

    def approximations2(self):
        if len(self.gunshots) < 3:
            return None, None
        first_reports = list(self.get_first_reports())[:10]
        ref = first_reports[0]
        data = []#[CoordinateCalculator.to_cart(ref.position, r.position) for r in first_reports]
        for r in first_reports:
            x,y,z = CoordinateCalculator.to_cart(ref.position, r.position)
            data.append((x,y,z,r.timestamp))
        self.position, self.timestamp = CoordinateCalculator.tdoa(np.array(data))
        self.position = CoordinateCalculator.from_cart(ref.position,self.position)
        return self.position, self.timestamp
    
    def approximations3(self):
        if len(self.gunshots) < 3:
            return None, None
        first_reports = list(self.get_first_reports())[:10]
        data = []
        for r in first_reports:
            x,y,z = r.position
            data.append((x,y,z,r.timestamp))
        self.position, self.timestamp = CoordinateCalculator.tdoa_spherical(np.array(data))
        return self.position, self.timestamp

class CoordinateCalculator:
    def distance(pos1: Pos, pos2: Pos):
        #Calculate distance in meters from just latitude and longitude
        flat_distance = geopy.distance.distance(pos1[:2], pos2[:2]).m
        #Take altitude into account using pythagorean theorem
        #This will not be accurate for very large distances due
        #to earth's curvature but will work for these purposes
        #with (relatively) short distances
        return math.sqrt(flat_distance**2 + (pos1[2] - pos2[2])**2)

    def cart_distance(pos1: Pos, pos2: Pos):
        return math.sqrt(sum((a-b)**2 for (a,b) in zip(pos1,pos2)))

    def from_cart(ref : Pos, cart : Pos):
        return (geopy.distance.distance(meters=cart[0]).destination(ref[:2],bearing=0)[0],
                geopy.distance.distance(meters=cart[1]).destination(ref[:2],bearing=90)[1],
                ref[2]+cart[2])

    def to_cart(ref : Pos, spherical : Pos):
        return (geopy.distance.distance(ref[:2],(spherical[0],ref[1])).m * np.sign(spherical[0]-ref[0]),
                geopy.distance.distance(ref[:2],(ref[0],spherical[1])).m * np.sign((spherical[1]-ref[1] % 360) - 180),
                spherical[2] - ref[2])

    def tdoa_spherical(data):
        def objective(x):
            return sum((CoordinateCalculator.distance(x[:3],xi[:3]) - SPEED_OF_SOUND_MS*(xi[3]-x[3]))**2 for xi in data)
        x0 = np.sum(data,axis=0)/len(data)
        bounds = ((-90,90),(-180,180),(None,None),(None,None))
        sol = scipy.optimize.minimize(objective, x0=x0, method="Nelder-Mead", bounds=bounds, tol=10^-3).x
        print("Solution:")
        print(sol,objective(sol))
        return sol[:3],int(sol[3])

    def tdoa(data):
        #print(gs1,gs2,gs3)
        #Using least squares linear approximation
        def linear_approximate():
            print("Data:\n", data)
            n = len(data)
            A = np.zeros((n*(n-1)//2,4))
            b = np.zeros((n*(n-1)//2,1))
            for k,(i,j) in enumerate((i,j) for i in range(n) for j in range(n) if j>i):
                A[k,:] = 2*(data[j]-data[i])
                A[k,-1] *= -SPEED_OF_SOUND_MS**2
                sqrs = np.square(data[j]) - np.square(data[i])
                sqrs[-1] *= -SPEED_OF_SOUND_MS**2
                b[k,0] = sum(sqrs)
            print("A:\n", A)
            print("b:\n", b)
            print("Linear estimate:\n",np.linalg.lstsq(A,b,rcond=None)[0][:,0])
            print(np.linalg.lstsq(A,b,rcond=None)[1])
            return np.linalg.lstsq(A,b,rcond=None)[0][:,0]

        def objective(x):
            return sum((CoordinateCalculator.cart_distance(x[:3],xi[:3]) - SPEED_OF_SOUND_MS*(xi[3]-x[3]))**2 for xi in data)
        def jacobian(x):
            return [
                    sum(2 * (CoordinateCalculator.cart_distance(x[:3],xi[:3]) - SPEED_OF_SOUND_MS*(xi[3]-x[3])) *
                    (x[i]-xi[i]) / CoordinateCalculator.cart_distance(x[:3],xi[:3])
                    for xi in data)
                    for i in range(3)
                ] + [
                    sum(2 * (CoordinateCalculator.cart_distance(x[:3],xi[:3]) - SPEED_OF_SOUND_MS*(xi[3]-x[3])) *
                    SPEED_OF_SOUND_MS
                    for xi in data)
                ]
        init = linear_approximate()
        print(objective(init))
        sol = scipy.optimize.minimize(objective, [43, -100, 4664,  -200], method="Nelder-Mead", tol=10^-3).x
        print("Solution:")
        print(sol,objective(sol))
        return sol[:3],sol[3]

    def midpoint(poslist):
        lats, longs, alts = zip(*poslist)
        l = len(poslist)
        return (sum(lats)/l, sum(longs)/l, sum(alts)/l)
            
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
        pos, time = event.approximations2()
        print("Approximate position: " + str(pos) + " timestamp: " + str(time))

def testfile(gunshotstrings):
    events = []
    for line in gunshotstrings:
        latitude, longitude, altitude, timestamp, weapontype, clientid = line.split(" ")[:6]
        gunshot = GunshotReport((float(latitude), float(longitude), float(altitude)), int(timestamp), weapontype, int(clientid))
        [popevent(event) for event in events if gunshot.timestamp - event.timestamp_latestshot > MAXTIME]
        events = [event for event in events if gunshot.timestamp - event.timestamp_latestshot <= MAXTIME]
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
        