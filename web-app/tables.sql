CREATE DATABASE IF NOT EXISTS pagd;
USE pagd;

CREATE TABLE Guns(
    name VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL
);

CREATE TABLE Reports(
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    timestamp  DATETIME(3) DEFAULT NOW(3) NOT NULL,
    coord_lat  FLOAT(23) NOT NULL,
    coord_long FLOAT(23) NOT NULL,
    coord_alt  FLOAT(23) NOT NULL,
    gun        VARCHAR(255) NOT NULL,
    FOREIGN KEY (gun) REFERENCES Guns(name)
);

CREATE TABLE GunshotEvents(
    gunshot_id INT,
    report     INT,
    timestamp  DATETIME(3),
    coord_lat  FLOAT(23),
    coord_long FLOAT(23),
    coord_alt  FLOAT(23),
    gun        VARCHAR(255) NOT NULL,
    shots_fired INT,
    PRIMARY KEY (gunshot_id, report),
    FOREIGN KEY (gun)    REFERENCES Guns(name),
    FOREIGN KEY (report) REFERENCES Reports(report_id)
);
