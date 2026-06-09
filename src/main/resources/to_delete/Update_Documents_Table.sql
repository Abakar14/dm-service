-- update uploads table 01.02.2026
ALTER TABLE uploads
    ADD COLUMN IF NOT EXISTS rendition_type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL';

ALTER TABLE uploads
    ADD COLUMN IF NOT EXISTS parent_upload_id VARCHAR(255);

ALTER TABLE uploads
    ADD COLUMN IF NOT EXISTS width INTEGER;
ALTER TABLE uploads
    ADD COLUMN IF NOT EXISTS height INTEGER;


CREATE INDEX IF NOT EXISTS idx_uploads_parent_upload_id ON uploads (parent_upload_id);
CREATE INDEX IF NOT EXISTS idx_uploads_rendition_type ON uploads (rendition_type);

