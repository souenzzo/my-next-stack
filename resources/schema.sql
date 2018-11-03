CREATE TABLE account (
  id        SERIAL NOT NULL UNIQUE PRIMARY KEY,
  slug      TEXT   NOT NULL UNIQUE,
  name      TEXT   NOT NULL
);

CREATE TABLE address (
  id     SERIAL NOT NULL UNIQUE PRIMARY KEY,
  street TEXT   NOT NULL
);

CREATE TABLE friends (
  id         SERIAL NOT NULL UNIQUE PRIMARY KEY,
  account_id SERIAL NOT NULL references account (id),
  friend_id  SERIAL NOT NULL references account (id)
);

CREATE TABLE addresses (
  id         SERIAL NOT NULL UNIQUE PRIMARY KEY,
  account_id SERIAL NOT NULL references account (id),
  address_id SERIAL NOT NULL references address (id)
);
