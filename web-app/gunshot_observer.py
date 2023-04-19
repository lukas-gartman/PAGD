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
        for event in self.events: # Try to find an event that fits with a report from the same client
            if event.try_sameclient(report):
                self.db.add_gunshot_report_relation(event.event_id, report_id)
            # break
        else: # Try to find an event that fits
            for event in self.events:
                if event.try_newclient(report):
                    approximations = event.approximations()
                    num_of_clients = len(event.clients)
                    if not None in approximations:
                        p, timestamp = approximations
                        lat, long, alt = p.v

                        if num_of_clients == GunshotEvent.MIN_CLIENTS:
                            self.db.add_gunshot(event.event_id, report_id, timestamp, lat, long, alt, event.weapontype, 1) # TODO: add shots_fired
                        elif num_of_clients > GunshotEvent.MIN_CLIENTS:
                            self.db.add_gunshot_report_relation(event.event_id, report_id)
                            self.db.update_gunshot(event.event_id, timestamp, lat, long, alt, event.weapontype, 1) # TODO: add shots_fired
                        else:
                            self.db.add_gunshot_report_relation(event.event_id, report_id)
                break
            else: # Create new event
                event = GunshotEvent(report)
                event.event_id = self.db.get_latest_gunshot_id() + 1
                self.events.add(event)
                self.db.add_temp_gunshot(event.event_id, report_id, event.weapontype)
