-- Created by Redgate Data Modeler (https://datamodeler.redgate-platform.com)
-- Patched for backend expectations: mystuff schema, image table, qr_token, and longer user fields.

-- extensions
-- Needed for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- schema
CREATE SCHEMA IF NOT EXISTS mystuff;

-- tables
-- Table: image  (backend expects mystuff.image)
CREATE TABLE IF NOT EXISTS mystuff.image (
  id serial NOT NULL,
  item_id int NOT NULL,
  image_data bytea NOT NULL,
  CONSTRAINT image_pk PRIMARY KEY (id)
);

-- Table: item
CREATE TABLE IF NOT EXISTS mystuff.item (
  id serial NOT NULL,
  user_id int NOT NULL,
  name varchar(50) NOT NULL,
  date date NOT NULL,
  model varchar(250) NULL,
  comment varchar(500) NULL,
  status varchar(1) NOT NULL,
  qr_token varchar(255) NULL,
  CONSTRAINT item_pk PRIMARY KEY (id)
);

-- qr_token: generate automatically on insert
ALTER TABLE mystuff.item
  ALTER COLUMN qr_token SET DEFAULT gen_random_uuid()::text;

-- Make qr_token mandatory (safe on fresh DB; also backfills existing rows if script re-run)
DO $$
BEGIN
  UPDATE mystuff.item
  SET qr_token = gen_random_uuid()::text
  WHERE qr_token IS NULL OR qr_token = '';

  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'mystuff'
      AND table_name = 'item'
      AND column_name = 'qr_token'
      AND is_nullable = 'YES'
  ) THEN
    ALTER TABLE mystuff.item
      ALTER COLUMN qr_token SET NOT NULL;
  END IF;
END $$;

-- Table: role
CREATE TABLE IF NOT EXISTS mystuff.role (
  id serial NOT NULL,
  name varchar(20) NOT NULL,
  CONSTRAINT role_pk PRIMARY KEY (id)
);

-- Table: user
CREATE TABLE IF NOT EXISTS mystuff."user" (
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

-- foreign keys
-- Reference: item_user (table: item)
ALTER TABLE mystuff.item
  ADD CONSTRAINT item_user
  FOREIGN KEY (user_id)
  REFERENCES mystuff."user" (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

-- Reference: image_item (table: image)
ALTER TABLE mystuff.image
  ADD CONSTRAINT image_item
  FOREIGN KEY (item_id)
  REFERENCES mystuff.item (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

-- Reference: user_role (table: user)
ALTER TABLE mystuff."user"
  ADD CONSTRAINT user_role
  FOREIGN KEY (role_id)
  REFERENCES mystuff.role (id)
  NOT DEFERRABLE INITIALLY IMMEDIATE;

-- helpful indexes
CREATE INDEX IF NOT EXISTS ix_item_user_id ON mystuff.item(user_id);
CREATE INDEX IF NOT EXISTS ix_image_item_id ON mystuff.image(item_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_item_qr_token ON mystuff.item(qr_token);

-- End of file.