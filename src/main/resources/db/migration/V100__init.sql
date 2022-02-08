CREATE TABLE Subscribers (
 botId UUID NOT NULL PRIMARY KEY,
 muted BOOL DEFAULT 'f',
 schedule VARCHAR,
 reminder VARCHAR
);
