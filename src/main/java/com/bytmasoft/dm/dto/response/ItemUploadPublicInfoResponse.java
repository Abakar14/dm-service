package com.bytmasoft.dm.dto.response;

/**
 * ItemUploadPublicInfoResponse
 *
 * @author Mahamat Abakar
 * @since 04.06.26
 */
public record ItemUploadPublicInfoResponse(
    Long itemId,
    String uploadId,
    String publicUrl,
    String thumbnailUrl,
    String previewUrl,
    String contentType
) {

}