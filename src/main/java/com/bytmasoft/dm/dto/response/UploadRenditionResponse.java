package com.bytmasoft.dm.dto.response;

/**
 * UploadRenditionResponse
 *
 * @author Mahamat Abakar
 * @since 03.05.26
 */
public record UploadRenditionResponse(
    String type,      // ORIGINAL, THUMBNAIL, PREVIEW
    String uploadId,
    String url,
    Long fileSize,
    String contentType,
    Integer width,
    Integer height
) {

}