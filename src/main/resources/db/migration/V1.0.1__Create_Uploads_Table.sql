-- Create documents table
CREATE TABLE IF NOT EXISTS uploads
(
    id                  VARCHAR(255) PRIMARY KEY,
    parent_upload_id    VARCHAR(255),
    added_by            character varying(255) COLLATE pg_catalog."default" NOT NULL,
    modified_by         character varying(255),
    file_name           VARCHAR(255)                                        NOT NULL,
    content_disposition VARCHAR(100),
    original_file_name  VARCHAR(255),
    content_type        VARCHAR(255),
    rendition_type      VARCHAR(20)                                         NOT NULL DEFAULT 'ORIGINAL',
    mime_type           VARCHAR(50),
    file_path           TEXT,
    upload_type         VARCHAR(50)                                         NOT NULL,
    file_size           BIGINT,
    width               INTEGER,
    height              INTEGER,
    version             INTEGER,
    item_id             BIGINT                                              NOT NULL,
    deleted             BOOLEAN                                                      DEFAULT FALSE,
    is_active           boolean                                             NOT NULL DEFAULT TRUE,
    is_archived         BOOLEAN                                                      DEFAULT FALSE,
    added_on            TIMESTAMP,
    modified_on         TIMESTAMP,
    domain_type         VARCHAR(25)                                         NOT NULL


);

-- Create indexes for better query performance
CREATE INDEX idx_uploads_owner_id ON uploads (item_id);
CREATE INDEX idx_uploads_upload_type ON uploads (upload_type);
CREATE INDEX idx_uploads_domain_type ON uploads (domain_type);
CREATE INDEX idx_uploads_is_active ON uploads (is_active);
CREATE INDEX idx_uploads_deleted ON uploads (deleted);
CREATE INDEX idx_uploads_added_on ON uploads (added_on);
CREATE INDEX IF NOT EXISTS idx_documents_file_name ON uploads (file_name);

CREATE INDEX idx_uploads_composite_search ON uploads (item_id, upload_type, is_active, deleted);
CREATE INDEX IF NOT EXISTS idx_uploads_parent_upload_id ON uploads (parent_upload_id);
CREATE INDEX IF NOT EXISTS idx_uploads_rendition_type ON uploads (rendition_type);

-- Comments
-- Comments
COMMENT ON TABLE uploads IS 'Stores all uploaded files information including images and documents';
COMMENT ON COLUMN uploads.upload_type IS 'Type of upload – plain string, validated in Java. Allowed values: USER_PROFILE_IMAGE, FOUND_ITEM_IMAGE, LOST_ITEM_IMAGE, LOST_ITEM_DOCUMENT, FOUND_ITEM_DOCUMENT, USER_DOCUMENT.';
COMMENT ON COLUMN uploads.domain_type IS 'Domain type – plain string, validated in Java. Allowed values: ITEM, VEHICLE, SCHOOL, OTHER.';
COMMENT ON COLUMN uploads.file_size IS 'File size in bytes';
COMMENT ON COLUMN uploads.version IS 'Version of file';