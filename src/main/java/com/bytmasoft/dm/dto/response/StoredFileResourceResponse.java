package com.bytmasoft.dm.dto.response;

import org.springframework.core.io.Resource;

public record StoredFileResourceResponse(
    String contentType,
    String etag,
    Resource resource
) {

}
