USE pagd;

-- Handguns --
INSERT IGNORE INTO Guns (name, type) VALUES
    ("Colt 1911", "Handgun"),
    ("Glock", "Handgun"),
    ("HK USP", "Handgun"),
    ("Kimber", "Handgun"),
    ("Lorcin", "Handgun"),
    ("Ruger", "Handgun"),
    ("SIG Sauer", "Handgun"),
    ("SpKing", "Handgun"),
    ("SW22", "Handgun"),
    ("SW38sp", "Handgun");

-- Rifles --
INSERT IGNORE INTO Guns (name, type) VALUES
    ("Bolt action", "Rifle"),
    ("M16", "Rifle"),
    ("MP40", "Rifle"),
    ("Remington 700", "Rifle"),
    ("Win M14", "Rifle"),
    ("WASR", "Rifle");
