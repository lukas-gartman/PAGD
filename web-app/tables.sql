CREATE DATABASE IF NOT EXISTS pagd;
USE pagd;

CREATE TABLE Guns(
    name VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    caliber VARCHAR(255) NOT NULL
);

CREATE TABLE Ammunitions(
    size VARCHAR(255),
    type VARCHAR(255),
    PRIMARY KEY (size, type)
);

CREATE TABLE Reports(
    report_id NUMERIC PRIMARY KEY,
    timestamp DATETIME(3) DEFAULT NOW(3) NOT NULL,
    coord VARCHAR(255) NOT NULL,
    gun VARCHAR(255) NOT NULL,
    bullet_size VARCHAR(255) NOT NULL,
    bullet_type VARCHAR(255) NOT NULL,
    FOREIGN KEY (gun) REFERENCES Guns(name),
    FOREIGN KEY (bullet_size, bullet_type) REFERENCES Ammunitions(size, type)
);

CREATE TABLE Gunshots(
    timestamp DATETIME(3) NOT NULL,
    coord VARCHAR(255) NOT NULL,
    gun VARCHAR(255) NOT NULL,
    bullet_size VARCHAR(255) NOT NULL,
    bullet_type VARCHAR(255) NOT NULL,
    PRIMARY KEY (timestamp, coord),
    FOREIGN KEY (gun) REFERENCES Guns(name),
    FOREIGN KEY (bullet_size, bullet_type) REFERENCES Ammunitions(size, type)
);