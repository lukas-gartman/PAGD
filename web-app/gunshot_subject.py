from subject_interface import SubjectInterface

class GunshotSubject(SubjectInterface):
    def __init__(self):
        self.observers = []
    
    def attach(self, observer):
        self.observers.append(observer)

    def detach(self, observer):
        self.observers.remove(observer)

    def notify(self, report):
        for observer in self.observers:
            observer.update(report)
