-- Schema for integration tests.
-- Drops and recreates the mystuff schema on every Spring context startup so that
-- ALTER TABLE / ADD CONSTRAINT statements are always fresh and never conflict
-- when multiple Spring contexts share the same Testcontainers database.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP SCHEMA IF EXISTS mystuff CASCADE;
CREATE SCHEMA mystuff;

CREATE TABLE mystuff.image (
  id serial NOT NULL,
  item_id int NOT NULL,
  image_data bytea NOT NULL,
  CONSTRAINT image_pk PRIMARY KEY (id)
);

CREATE TABLE mystuff.item (
  id serial NOT NULL,
  user_id int NOT NULL,
  name varchar(50) NOT NULL,
  date date NOT NULL,
  model varchar(250) NULL,
  comment varchar(500) NULL,
  status varchar(1) NOT NULL,
  qr_token varchar(255) NOT NULL DEFAULT gen_random_uuid()::text,
  warranty_end_date date NULL,
  warranty_notify_at timestamptz NULL,
  CONSTRAINT item_pk PRIMARY KEY (id)
);

CREATE TABLE mystuff.role (
  id serial NOT NULL,
  name varchar(20) NOT NULL,
  CONSTRAINT role_pk PRIMARY KEY (id)
);

CREATE TABLE mystuff."user" (
  id serial NOT NULL,
  role_id int NOT NULL,
  username varchar(255) NOT NULL,
  password varchar(255) NULL,
  email varchar(255) NOT NULL,
  status varchar(1) NOT NULL,
  google_uid varchar(255) NULL,
  CONSTRAINT user_pk PRIMARY KEY (id),
  CONSTRAINT ux_user_email UNIQUE (email),
  CONSTRAINT ux_user_google_uid UNIQUE (google_uid)
);

ALTER TABLE mystuff.item
  ADD CONSTRAINT item_user
  FOREIGN KEY (user_id)
  REFERENCES mystuff."user" (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE mystuff.image
  ADD CONSTRAINT image_item
  FOREIGN KEY (item_id)
  REFERENCES mystuff.item (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE mystuff."user"
  ADD CONSTRAINT user_role
  FOREIGN KEY (role_id)
  REFERENCES mystuff.role (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

CREATE INDEX ix_item_user_id ON mystuff.item(user_id);
CREATE INDEX ix_image_item_id ON mystuff.image(item_id);
CREATE UNIQUE INDEX ux_item_qr_token ON mystuff.item(qr_token);

CREATE TABLE mystuff.password_reset_token (
  id          serial        NOT NULL,
  user_id     int           NOT NULL,
  token       varchar(36)   NOT NULL,
  expires_at  timestamptz   NOT NULL,
  used        boolean       NOT NULL DEFAULT false,
  created_at  timestamptz   NOT NULL DEFAULT now(),
  CONSTRAINT password_reset_token_pk PRIMARY KEY (id),
  CONSTRAINT ux_password_reset_token UNIQUE (token)
);

ALTER TABLE mystuff.password_reset_token
  ADD CONSTRAINT prt_user
  FOREIGN KEY (user_id)
  REFERENCES mystuff."user" (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

CREATE INDEX ix_prt_token ON mystuff.password_reset_token(token);
CREATE INDEX ix_prt_user_id ON mystuff.password_reset_token(user_id);

ALTER TABLE mystuff."user"
  ADD COLUMN last_login_at timestamptz NULL;

ALTER TABLE mystuff."user"
  ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE mystuff."user"
  ADD COLUMN auth_provider varchar(10) NOT NULL DEFAULT 'PASSWORD';
