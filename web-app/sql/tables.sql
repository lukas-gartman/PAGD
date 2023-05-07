CREATE DATABASE IF NOT EXISTS pagd;
USE pagd;

-- TABLES --
CREATE OR REPLACE TABLE Guns(
    name VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL
);

CREATE OR REPLACE TABLE Reports(
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    timestamp BIGINT NOT NULL,
    coord     POINT NOT NULL,
    altitude  FLOAT(5,1) NOT NULL,
    gun       VARCHAR(255) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (gun) REFERENCES Guns(name)
);

CREATE OR REPLACE TABLE Gunshots(
    gunshot_id  INT PRIMARY KEY,
    timestamp   BIGINT,
    coord       POINT,
    altitude    FLOAT(5,1),
    gun         VARCHAR(255) NOT NULL,
    shots_fired INT,
    FOREIGN KEY (gun) REFERENCES Guns(name)
);

CREATE OR REPLACE TABLE GunshotReports(
    gunshot_id INT REFERENCES Gunshots,
    report_id  INT UNIQUE REFERENCES Reports,
    PRIMARY KEY (gunshot_id, report_id)
);

-- Views --
CREATE OR REPLACE VIEW ReportsView AS
    SELECT report_id, timestamp, X(coord) AS coord_lat, Y(coord) AS coord_long, altitude AS coord_alt, gun, client_id
    FROM Reports;

CREATE OR REPLACE VIEW GunshotsView AS
    SELECT gunshot_id, timestamp, X(coord) AS coord_lat, Y(coord) AS coord_long, altitude AS coord_alt, gun, shots_fired
    FROM Gunshots;

CREATE OR REPLACE VIEW GunshotEventsWithReports AS
    SELECT G.gunshot_id, R.report_id, G.timestamp, X(G.coord) AS coord_lat, Y(G.coord) AS coord_long, G.altitude AS coord_alt, G.gun, G.shots_fired
    FROM Gunshots AS G, Reports AS R;
