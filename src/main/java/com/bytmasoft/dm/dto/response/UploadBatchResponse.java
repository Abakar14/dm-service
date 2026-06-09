package com.bytmasoft.dm.dto.response;

import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UploadBatchResponseDTO
 *
 * @author Mahamat Abakar
 * @since 14.02.26
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadBatchResponse {

  private Long itemId;                 // ownerId
  private String mode;                 // FOUND/LOST (optional at DM level)
  private DomainType domainType;
  private UploadType uploadType;

  private Integer received;
  private Integer successCount;
  private Integer failedCount;

  private List<UploadFileResultResponse> results;
}
