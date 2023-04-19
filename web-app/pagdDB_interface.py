from abc import ABC, abstractmethod

class PagdDBInterface(ABC):
    @abstractmethod
    def add_gun(self, gun_name, gun_type):
        pass
    
    @abstractmethod
    def get_gun(self, gun_name):
        pass
    
    @abstractmethod
    def add_report(self, timestamp, coord_lat, coord_long, coord_alt, gun):
        pass
    
    @abstractmethod
    def get_report(self, report_id):
        pass
    
    @abstractmethod
    def get_report_range(self, time_from, time_to):
        pass
    
    @abstractmethod
    def add_gunshot(self, gunshot_id, report_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired):
        pass
    
    @abstractmethod
    def add_temp_gunshot(self, gunshot_id, report_id, gun):
        pass
    
    @abstractmethod
    def update_gunshot(self, gunshot_id, timestamp, coord_lat, coord_long, coord_alt, gun, shots_fired):
        pass
    
    @abstractmethod
    def get_gunshot_by_id(self, gunshot_id):
        pass
    
    @abstractmethod
    def get_gunshots_by_timestamp(self, time_from, time_to):
        pass
    
    @abstractmethod
    def get_all_gunshots(self):
        pass
    
    @abstractmethod
    def get_latest_gunshot_id(self):
        pass
