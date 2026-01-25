-- Step 7 – Full Text Search for Audio
-- Uses GENERATED COLUMN (no trigger, no manual update)

-- 1️⃣ Add generated search_vector column
ALTER TABLE audio
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                    'english',
                    coalesce(title, '') || ' ' || coalesce(description, '')
            )
            ) STORED;

-- 2️⃣ Create GIN index for fast full-text search
CREATE INDEX idx_audio_search_vector
    ON audio
    USING GIN (search_vector);
