-- Add image columns to posts. Backfill existing rows with a placeholder so the
-- NOT NULL constraint can be applied without breaking current data or the UI.
ALTER TABLE posts ADD COLUMN image_url       VARCHAR(500);
ALTER TABLE posts ADD COLUMN image_public_id VARCHAR(255);

UPDATE posts
   SET image_url = 'https://res.cloudinary.com/demo/image/upload/v1/community-board/placeholder.png'
 WHERE image_url IS NULL;

ALTER TABLE posts ALTER COLUMN image_url SET NOT NULL;
