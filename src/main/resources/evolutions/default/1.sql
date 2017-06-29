# --- !Ups

CREATE TABLE "person" (
  name: VARCHAR(255),
  birthDate: TIMESTAMP not null,
  deathDate: TIMESTAMP,
  id BIGSERIAL PRIMARY KEY
);


# --- !Downs

DROP TABLE IF EXISTS "person" CASCADE;
