package com.bytmasoft.dm.dto.response;

import com.bytmasoft.dm.enums.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UploadFileResultDTO
 *
 * @author Mahamat Abakar
 * @since 14.02.26
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileResultResponse {

  private String clientId;           // optional: from frontend (for mapping UI cards)
  private String originalFileName;
  private Long fileSize;
  private String mimeType;

  private UploadStatus status;       // SUCCESS | FAILED

  // on success
  private UploadEntityResponse entity;

  // on failure
  private String errorCode;          // e.g. INVALID_TYPE, FILE_TOO_LARGE, ...
  private String message;
}
