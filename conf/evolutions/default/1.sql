# --- !Ups
CREATE TABLE `user` (
    email VARCHAR(255) UNIQUE NOT NULL,
    password TINYBLOB NOT NULL,
    salt TINYBLOB NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (email)
);

# --- !Downs
DROP TABLE `user`;