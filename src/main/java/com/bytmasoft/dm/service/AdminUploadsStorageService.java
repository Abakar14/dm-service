package com.bytmasoft.dm.service;

import com.bytmasoft.dm.dto.page.PagedResponse;
import com.bytmasoft.dm.dto.response.TrashPurgeResponse;
import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.exception.StorageFileNotFoundException;
import com.bytmasoft.dm.mapper.UploadEntityMapper;
import com.bytmasoft.dm.repository.UploadEntityRepository;
import com.bytmasoft.dm.repository.UploadEntitySpecification;
import com.bytmasoft.dm.service.storage.StorageService;
import com.bytmasoft.dm.util.DMUtils;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminUploadsStorageService {

  private final StorageService storageService;
  private final UploadEntityRepository entityRepository;
  private final UploadEntitySpecification uploadEntitySpecification;
  private final UploadEntityMapper mapper;
  private final DMUtils dmUtils;

  public boolean permanentlyDelete(String uploadId) throws IOException {
    UploadEntity uploadEntity = findUploadEntityId(uploadId);

    // delete cached thumbnail (best effort)
    try {
      deleteThumbnailFor(uploadId);
    } catch (IOException ex) {
      // log and continue; original delete is the important part
      log.warn("Failed to delete thumbnail for uploadId={}", uploadId, ex);
    }

    boolean result = storageService.delete(uploadEntity.getFilePath(), uploadEntity.getFileName());
    entityRepository.delete(uploadEntity);
    return result;
  }


  public TrashPurgeResponse purgeTrash(DomainType domainType, int olderThanDays)
      throws IOException {
    if (olderThanDays < 1) {
      throw new IllegalArgumentException("olderThanDays must be >= 1");
    }
    int deletedCount = storageService.purgeTrash(domainType, olderThanDays);
    return new TrashPurgeResponse(deletedCount, olderThanDays);
  }


  /************************************ Noch nicht getestet ****************************************************/


  private UploadEntity findUploadEntityId(String uploadId) {
    return entityRepository.findById(uploadId).orElseThrow(() -> new StorageFileNotFoundException(
        "UploadEntity with id: " + uploadId + " not found"));
  }


  //TODO Implementation
  public UploadEntityResponse restoreFile(String uploadId) {
    //move back from .trash
    //set deleted =false;
    return null;
  }

  public PagedResponse<UploadEntityResponse> getAllDocuments(DomainType domainType,
      UploadType uploadType, Long ownerId, Pageable pageable) {

    Specification<UploadEntity> spec = uploadEntitySpecification.getSpecificationByDomainTypeByUploadTypeOwnerIdAndVersion(
        domainType, uploadType, ownerId, null);

    Page<UploadEntity> documentPage = entityRepository.findAll(spec, pageable);

    return PagedResponse.of(documentPage.map(mapper::toDto));
  }

  private void deleteThumbnailFor(String id) throws IOException {
    UploadEntity ent = findUploadEntityId(id);

    // Only for image types where thumb makes sense
    UploadType thumbType = dmUtils.mapToThumbType(ent.getUploadType());
    if (thumbType == ent.getUploadType()) {
      return; // no mapped thumbnail type => do nothing
    }

    // If your LocalFsStorageService stores thumbs under ".thumbs" near the original,
    // deleting the thumb is done via a dedicated storage method:
    storageService.deleteThumbnail(ent.getFilePath(), ent.getFileName());
  }


}
