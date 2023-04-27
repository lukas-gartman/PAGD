from threading import Lock

from observer_interface import ObserverInterface
from subject_interface import SubjectInterface
from pagdDB_interface import PagdDBInterface
from gunshot import GunshotReport, GunshotEvent

class GunshotObserver(ObserverInterface):
    def __init__(self, subject: SubjectInterface, db: PagdDBInterface):
        self.subject = subject
        self.subject.attach(self)
        self.db = db
        self.events = set()
        self.gunshot_report = None
        self.lock = Lock()

    def update(self, report):
        report_id = report[0]
        position = report[1]
        timestamp = report[2]
        weapontype = report[3]
        clientid = report[4]

        gunshot_report = GunshotReport.from_coordinates(position, timestamp, weapontype, clientid)
        self._add_gunshot(report_id, gunshot_report)

    def detach(self):
        self.subject.detach(self)

    def _add_gunshot(self, report_id, report):
        with self.lock:
            for event in self.events: # Try to find an event that fits with a report from the same client
                if event.fits(report) and event.client_has_added(report):
                    event.add_report(report)
                    self.db.add_gunshot_report_relation(event.event_id, report_id)
                    break # Since it has found an event
            else: # Try to find an event that fits
                for event in self.events:
                    if event.fits(report) and not event.client_has_added(report):
                        event.add_report(report)
                        p, timestamp = event.approximations() # May be None if clients < 3 or position could not be determined
                        num_of_clients = len(event.clients)
                        if p is not None:
                            lat, long, alt = p.v
                        else:
                            lat, long, alt = None, None, None

                        if num_of_clients == GunshotEvent.MIN_CLIENTS:
                            self.db.add_gunshot(event.event_id, report_id, timestamp, lat, long, alt, event.weapontype, event.total_firings()) # TODO: add shots_fired
                        elif num_of_clients > GunshotEvent.MIN_CLIENTS:
                            self.db.add_gunshot_report_relation(event.event_id, report_id)
                            self.db.update_gunshot(event.event_id, timestamp, lat, long, alt, event.weapontype, event.total_firings()) # TODO: add shots_fired
                        else:
                            self.db.add_gunshot_report_relation(event.event_id, report_id)
                        break # Since it has found an event
                else: # Create new event
                    print("creating new event")
                    event = GunshotEvent(report)
                    event.event_id = self.db.get_latest_gunshot_id() + 1
                    print("id:", event.event_id)
                    self.events.add(event)
                    self.db.add_temp_gunshot(event.event_id, report_id, event.weapontype)
