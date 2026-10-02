-- Run once against databases created before Google sign-in was added.
-- spring.jpa.hibernate.ddl-auto=update adds new columns but never drops or relaxes old ones,
-- so the old schema keeps rejecting OAuth2 users (no password / phone / region / date of birth yet).
--
-- Long-term fix: replace ddl-auto=update with Flyway or Liquibase migrations.

ALTER TABLE customer DROP COLUMN IF EXISTS username;
ALTER TABLE customer ALTER COLUMN password      DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN phone_number  DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN region        DROP NOT NULL;
ALTER TABLE customer ALTER COLUMN date_of_birth DROP NOT NULL;
-- Product.imageUrl was replaced by the PRODUCT_IMAGE table.
ALTER TABLE product  DROP COLUMN IF EXISTS image_url;
