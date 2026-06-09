package com.bytmasoft.dm.dto.response;

public record ImageRefResponse(
    String id,
    String fileName,
    String contentType,
    long size,
    Integer width,
    Integer height,
    // Integer thumbWidth,
    // Integer thumbHeight,
    String downloadUrl,
    String thumbUrl
) {

}

