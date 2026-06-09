package com.bytmasoft.dm.mapper;

import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import com.bytmasoft.dm.dto.response.UploadFileResultResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.UploadStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadEntityMapper {

  public UploadEntityResponse toDto(UploadEntity uploadEntity) {

    if (uploadEntity == null) {
      return null;
    }

    return UploadEntityResponse.builder()
        .id(uploadEntity.getId())
        .uploadType(uploadEntity.getUploadType())
        .version(uploadEntity.getVersion())
        .ownerId(uploadEntity.getItemId())
        .originalFileName(uploadEntity.getOriginalFileName())
        .modifiedBy(uploadEntity.getModifiedBy())
        .fileSize(uploadEntity.getFileSize())
        .mimeType(uploadEntity.getMimeType())
        .isArchived(uploadEntity.isArchived())
        .filePath(uploadEntity.getFilePath())
        .isActive(uploadEntity.getIsActive())
        .fileName(uploadEntity.getFileName())
        .domainType(uploadEntity.getDomainType())
        .contentType(uploadEntity.getContentType())
        .addedOn(uploadEntity.getAddedOn())
        .deleted(uploadEntity.isDeleted())
        .addedBy(uploadEntity.getAddedBy())
        .build();
  }

  public UploadFileResultResponse toUploadFileResultDTO(UploadEntity uploadEntity) {
    if (uploadEntity == null) {
      return null;
    }

    return UploadFileResultResponse.builder()
        .clientId(null) // optional (set from request if you support it)
        .originalFileName(uploadEntity.getOriginalFileName())
        .fileSize(uploadEntity.getFileSize())
        .mimeType(uploadEntity.getMimeType())
        .status(UploadStatus.SUCCESS)
        .entity(toDto(uploadEntity))
        .errorCode(null)
        .message(null)
        .build();
  }

  public UploadFileResultResponse toFailedUploadFileResultDTO(MultipartFile file, String clientId,
      String errorCode,
      String message) {
    return UploadFileResultResponse.builder()
        .clientId(clientId)
        .originalFileName(file != null ? file.getOriginalFilename() : null)
        .fileSize(file != null ? file.getSize() : null)
        .mimeType(file != null ? file.getContentType() : null)
        .status(UploadStatus.FAILED)
        .entity(null)
        .errorCode(errorCode)
        .message(message)
        .build();
  }

}
