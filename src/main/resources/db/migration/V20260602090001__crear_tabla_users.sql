CREATE TABLE IF NOT EXISTS users (
    id       bigserial    NOT NULL,
    username varchar(255) NOT NULL,
    password varchar(255) NOT NULL,
    enabled  boolean      NOT NULL DEFAULT true,
    role     varchar(50)  NOT NULL DEFAULT 'USER',
    PRIMARY KEY (id),
    CONSTRAINT uq_username UNIQUE (username)
);
