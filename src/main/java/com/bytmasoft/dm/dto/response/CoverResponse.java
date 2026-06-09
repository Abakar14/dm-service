package com.bytmasoft.dm.dto.response;

public record CoverResponse(String orignalId, String thumbId, String downloadUrl, String thumbUrl,
                            int imageCount) {

}
