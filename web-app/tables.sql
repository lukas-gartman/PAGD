CREATE DATABASE IF NOT EXISTS pagd;
USE pagd;

-- TABLES --
CREATE OR REPLACE TABLE Guns(
    name VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL
);

CREATE OR REPLACE TABLE Reports(
    report_id  INT PRIMARY KEY AUTO_INCREMENT,
    timestamp  BIGINT NOT NULL,
    coord_lat  FLOAT(23) NOT NULL,
    coord_long FLOAT(23) NOT NULL,
    coord_alt  FLOAT(23) NOT NULL,
    gun        VARCHAR(255) NOT NULL,
    FOREIGN KEY (gun) REFERENCES Guns(name)
);

CREATE OR REPLACE TABLE Gunshots(
    gunshot_id  INT PRIMARY KEY,
    timestamp   BIGINT,
    coord_lat   FLOAT(23),
    coord_long  FLOAT(23),
    coord_alt   FLOAT(23),
    gun         VARCHAR(255) NOT NULL,
    shots_fired INT,
    FOREIGN KEY (gun) REFERENCES Guns(name)
);

CREATE OR REPLACE TABLE GunshotReports(
    gunshot_id INT,
    report_id INT,
    PRIMARY KEY (gunshot_id, report_id),
    FOREIGN KEY (gunshot_id) REFERENCES Gunshots(gunshot_id),
    FOREIGN KEY (report_id)  REFERENCES Reports(report_id)
);

