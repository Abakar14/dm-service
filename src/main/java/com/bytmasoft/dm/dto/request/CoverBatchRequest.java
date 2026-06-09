package com.bytmasoft.dm.dto.request;

import com.bytmasoft.dm.enums.UploadType;
import java.util.List;

public record CoverBatchRequest(UploadType uploadType, List<Long> ownerIds) {

}
