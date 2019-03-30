CREATE TABLE app_user
(
    id       SERIAL UNIQUE NOT NULL PRIMARY KEY,
    username TEXT UNIQUE   NOT NULL
);

CREATE TABLE app_session
(
    id     SERIAL UNIQUE NOT NULL PRIMARY KEY,
    csrf   TEXT UNIQUE   NOT NULL,
    authed INTEGER references app_user (id)
);


CREATE TABLE app_chat
(
    id    SERIAL UNIQUE NOT NULL PRIMARY KEY,
    title TEXT
);

CREATE TABLE app_user_chat
(
    id    SERIAL UNIQUE NOT NULL PRIMARY KEY,
    owner SERIAL        NOT NULL references app_user (id),
    chat  SERIAL        NOT NULL references app_chat (id)
);

CREATE TABLE app_message
(
    id     SERIAL UNIQUE NOT NULL PRIMARY KEY,
    author SERIAL        NOT NULL references app_user (id),
    chat   SERIAL        NOT NULL references app_chat (id),
    body   TEXT          NOT NULL
);
