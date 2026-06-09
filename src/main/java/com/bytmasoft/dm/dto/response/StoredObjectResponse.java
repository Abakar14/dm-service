package com.bytmasoft.dm.dto.response;

public record StoredObjectResponse(
    String directory,
    String filename,
    String originalFilename,
    String contentType,
    long sizeBytes

) {

}
