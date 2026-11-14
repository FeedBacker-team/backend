ALTER TABLE feedback_posts ADD COLUMN complete_at TIMESTAMP(6);

UPDATE feedback_posts
SET complete_at = updated_at
WHERE status = 'COMPLETED'
  AND complete_at IS NULL;
