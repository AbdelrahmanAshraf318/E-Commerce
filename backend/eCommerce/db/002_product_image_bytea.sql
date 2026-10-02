-- Run once, with the backend STOPPED, before starting a build where ProductImage.imageData is no longer @Lob.
-- Converts PRODUCT_IMAGE.IMAGE_DATA from OID (large object) to BYTEA.
--
-- The existing rows only hold dev-seed images, so they are dropped rather than converted;
-- re-run db/dev_seed_product_images.sql afterwards.

BEGIN;

-- Free the large objects first: deleting the rows alone would leave their bytes in pg_largeobject forever.
SELECT lo_unlink(image_data) FROM product_image WHERE image_data IS NOT NULL;
DELETE FROM product_image;

ALTER TABLE product_image ALTER COLUMN image_data TYPE bytea USING NULL;

COMMIT;
