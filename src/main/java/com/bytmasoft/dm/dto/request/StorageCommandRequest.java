package com.bytmasoft.dm.dto.request;

import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;


/**
 * Storage intent for placing an object in the storage backend.
 *
 * <p>We keep these fields aligned with existing API parameters (domain/owner/document types).
 */
public record StorageCommandRequest(
    DomainType domainType,
    UploadType uploadType,
    Long ownerId,
    int version,
    String originalFilename,
    String contentType
) {

}
