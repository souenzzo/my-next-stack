CREATE TABLE account (
    id SERIAL UNIQUE NOT NULL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL
);

CREATE TABLE chat (
    id SERIAL UNIQUE NOT NULL PRIMARY KEY,
    name TEXT
);

CREATE TABLE account_chat (
    id SERIAL UNIQUE NOT NULL PRIMARY KEY,
    account SERIAL NOT NULL references account(id),
    chat SERIAL NOT NULL references chat(id)
);

CREATE TABLE message (
    id SERIAL UNIQUE NOT NULL PRIMARY KEY,
    author SERIAL NOT NULL references account(id),
    body TEXT NOT NULL
);
