ALTER TABLE uploads
    ADD CONSTRAINT uk_upload_owner_type_version
        UNIQUE (domain_type, upload_type, item_id, version);