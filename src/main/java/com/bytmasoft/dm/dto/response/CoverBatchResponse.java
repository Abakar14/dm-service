package com.bytmasoft.dm.dto.response;

import java.util.Map;

public record CoverBatchResponse(Map<Long, CoverResponse> covers) {

}
