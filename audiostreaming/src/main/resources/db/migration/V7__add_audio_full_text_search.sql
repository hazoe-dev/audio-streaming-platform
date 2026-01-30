-- Step 7 – Full Text Search for Audio
-- Uses GENERATED COLUMN (no trigger, no manual update)

-- 1. Add generated search_vector column
ALTER TABLE audio
DROP COLUMN search_vector;

ALTER TABLE audio
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                    'english',
                    coalesce(title, '') || ' ' || coalesce(description, '')
            )
            ) STORED;


-- 2. Create GIN index for fast full-text search
CREATE INDEX idx_audio_search_vector
    ON audio
    USING GIN (search_vector);

-- 3. Update audio with search_vector
UPDATE audio SET title = title;
UPDATE audio SET description = description;