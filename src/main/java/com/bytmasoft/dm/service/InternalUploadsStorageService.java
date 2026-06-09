package com.bytmasoft.dm.service;

import com.bytmasoft.dm.context.RequestContext;
import com.bytmasoft.dm.context.RequestContextData;
import com.bytmasoft.dm.dto.page.PagedResponse;
import com.bytmasoft.dm.dto.request.StorageCommandRequest;
import com.bytmasoft.dm.dto.response.OptimizedImageResponse;
import com.bytmasoft.dm.dto.response.StoredObjectResponse;
import com.bytmasoft.dm.dto.response.UploadBatchResponse;
import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import com.bytmasoft.dm.dto.response.UploadFileResultResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.RenditionType;
import com.bytmasoft.dm.enums.UploadStatus;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.exception.InvalidFileException;
import com.bytmasoft.dm.exception.InvalidRequestException;
import com.bytmasoft.dm.exception.StorageException;
import com.bytmasoft.dm.exception.enums.DmErrorCode;
import com.bytmasoft.dm.mapper.UploadEntityMapper;
import com.bytmasoft.dm.repository.UploadEntityRepository;
import com.bytmasoft.dm.repository.UploadEntitySpecification;
import com.bytmasoft.dm.service.helper.FileValidationService;
import com.bytmasoft.dm.service.storage.FileMetaResolver;
import com.bytmasoft.dm.service.storage.FileMetaResolver.FileMeta;
import com.bytmasoft.dm.service.storage.StorageService;
import com.bytmasoft.dm.util.DMUtils;
import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


//TODO Adapt a new Exception to a new DmErrorCode exception
@Slf4j
@RequiredArgsConstructor
@Service
public class InternalUploadsStorageService {

  private static final Logger logger = LoggerFactory.getLogger(
      InternalUploadsStorageService.class);


  private final StorageService storageService;
  private final UploadEntityMapper uploadEntityMapper;
  private final UploadEntityRepository uploadEntityRepository;
  private final FileValidationService fileValidationService;
  private final UploadEntitySpecification uploadEntitySpecification;
  private final DMUtils dmUtils;
  private final UploadEntityMapper mapper;
  private final FileMetaResolver fileMetaResolver;
  private final ThumbnailService thumbnailService;

  public PagedResponse<UploadEntityResponse> getAllDocuments(DomainType domainType,
      UploadType uploadType, Long ownerId, Pageable pageable) {

    Specification<UploadEntity> spec = uploadEntitySpecification.getSpecificationByDomainTypeByUploadTypeOwnerIdAndVersion(
        domainType, uploadType, ownerId, null);

    Page<UploadEntity> documentPage = uploadEntityRepository.findAll(spec, pageable);

    return PagedResponse.of(documentPage.map(mapper::toDto));
  }

  public Resource downloadDocumentById(String uploadId) throws IOException {
    /*    UploadEntity uploadEntity = findUploadEntityId(uploadId);*/
    FileMeta fileMeta = fileMetaResolver.resolve(uploadId);
    return storageService.load(fileMeta.directory(), fileMeta.filename());
  }

  public Page<UploadEntityResponse> getAllDocumentsOwners(DomainType domainType,
      UploadType uploadType, List<Long> ownerIDs, Integer version, Pageable pageable) {
    Specification spec = uploadEntitySpecification.getDocumentsOwnerListByUploadTypeAndVersion(
        domainType, uploadType, ownerIDs, version);

    Page<UploadEntity> page = uploadEntityRepository.findAll(spec, pageable);

    return page.map(uploadEntityMapper::toDto);

  }

  public UploadEntityResponse getUploadsById(String uploadId) {
    return uploadEntityMapper.toDto(findUploadEntityId(uploadId));
  }

  public Resource downloadDocument(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version) throws IOException {
    UploadEntity uploadEntity = findDocumentBy(domainType, uploadType, ownerId, version);
    return storageService.load(uploadEntity.getFilePath(), uploadEntity.getFileName());
  }

  public ByteArrayResource downloadAllDocumentsAsZip(DomainType domainType,
      UploadType uploadType, Long ownerId, Integer version) throws IOException {

    Specification<UploadEntity> spec = uploadEntitySpecification.getSpecificationByDomainTypeByUploadTypeOwnerIdAndVersion(
        domainType, uploadType, ownerId, version);

    List<UploadEntity> uploadEntities = uploadEntityRepository.findAll(spec);
    return storageService.generateZip(domainType, uploadType, ownerId, version, uploadEntities);
  }


  public UploadFileResultResponse uploadSingleFile(
      MultipartFile file,
      DomainType domainType,
      UploadType uploadType,
      Long ownerId,
      String clientId,
      Integer version
  ) {

    final String origiName = (file != null ? file.getOriginalFilename() : null);

    try {
      // 0) validate early (throws InvalidFileException)
      FileValidationService.ValidatedUpload v = fileValidationService.validateAndRead(file,
          uploadType, ownerId);

      byte[] inputBytes = v.bytes();
      String detectedType = v.detectedMimeType();
      String safeName = v.safeFileName();
      String originalName = v.originalFileName();

      // 1) versioning

      // 2) content-type normalization (avoid octet-stream/bin cases)
      //String contentType = dmUtils.normalizeContentType(detectedType, originalName);
      boolean isImage = thumbnailService.isImage(detectedType);

      // 3) For NON-images: store as-is (stream), no thumb
      if (!isImage) {
        StorageCommandRequest cmd = new StorageCommandRequest(
            domainType, uploadType, ownerId, version,
            dmUtils.safeFilename(originalName, detectedType),
            detectedType
        );

        StoredObjectResponse obj = storageService.store(file.getInputStream(), cmd);

        UploadEntity entity = UploadEntity.builder()
            .fileName(obj.filename())
            .itemId(ownerId)
            .originalFileName(obj.originalFilename())
            .filePath(obj.directory())
            .fileSize(file.getSize())
            .width(null)
            .height(null)
            .version(version)
            .build();

        entity.setDomainType(domainType);
        entity.setUploadType(uploadType);
        entity.setAddedBy(dmUtils.getUsername());
        entity.setContentType(detectedType);
        entity.setMimeType(detectedType);
        entity.setRenditionType(RenditionType.ORIGINAL);

        entity = uploadEntityRepository.save(entity);



        return UploadFileResultResponse.builder()
            .clientId(clientId)
            .originalFileName(originalName)
            .fileSize(entity.getFileSize())
            .mimeType(detectedType)
            .status(UploadStatus.SUCCESS)
            .entity(uploadEntityMapper.toDto(entity))
            .build();
      }

      // 4) IMAGE path: buffer once, generate optimized bytes once
      // Optimize: resize/recompress; output MUST be a supported image type (recommend JPEG/WebP)
      OptimizedImageResponse optimized = thumbnailService.optimizeOriginal(
          new ByteArrayInputStream(inputBytes),
          detectedType,
          safeName
      );

      // Use optimized bytes + type + filename
      byte[] optimizedBytes = optimized.bytes();
      String optimizedType = optimized.contentType();         // e.g. image/jpeg
      String optimizedName = optimized.fileName();            // e.g. photo.jpg

      Dimension dim = dmUtils.readImageSize(optimizedBytes);

      // 5) store optimized original
      StorageCommandRequest originalCmd = new StorageCommandRequest(
          domainType, uploadType, ownerId, version,
          optimizedName,
          optimizedType
      );

      StoredObjectResponse originalObj = storageService.store(
          new ByteArrayInputStream(optimizedBytes),
          originalCmd
      );

      UploadEntity originalEntity = UploadEntity.builder()
          .fileName(originalObj.filename())
          .itemId(ownerId)
          .originalFileName(originalObj.originalFilename()) // store optimized filename
          .filePath(originalObj.directory())
          .fileSize((long) optimizedBytes.length)          // IMPORTANT: stored size
          .height(dim != null ? dim.height : null)
          .width(dim != null ? dim.width : null)
          .version(version)
          .build();

      originalEntity.setDomainType(domainType);
      originalEntity.setUploadType(uploadType);
      originalEntity.setAddedBy(dmUtils.getUsername());
      originalEntity.setContentType(optimizedType);
      originalEntity.setMimeType(optimizedType);
      originalEntity.setRenditionType(RenditionType.ORIGINAL);

      originalEntity = uploadEntityRepository.save(originalEntity);

      // 6) thumbnail (optional but recommended for UI lists)
      byte[] thumbJpeg = thumbnailService.generateJpeg(new ByteArrayInputStream(optimizedBytes));
      Dimension thumbDim = dmUtils.readImageSize(thumbJpeg);

      UploadType thumbType = dmUtils.mapToThumbType(uploadType);
      StorageCommandRequest thumbCmd = new StorageCommandRequest(
          domainType, thumbType, ownerId, version,
          "thumb.jpg",
          "image/jpeg"
      );

      StoredObjectResponse thumbObj = storageService.store(new ByteArrayInputStream(thumbJpeg),
          thumbCmd);

      UploadEntity thumbEntity = UploadEntity.builder()
          .fileName(thumbObj.filename())
          .itemId(ownerId)
          .originalFileName("thumb.jpg")
          .filePath(thumbObj.directory())
          .fileSize((long) thumbJpeg.length)
          .width(thumbDim != null ? thumbDim.width : null)
          .height(thumbDim != null ? thumbDim.height : null)
          .version(version)
          .build();

      thumbEntity.setDomainType(domainType);
      thumbEntity.setUploadType(thumbType);
      thumbEntity.setAddedBy(dmUtils.getUsername());
      thumbEntity.setMimeType("image/jpeg");
      thumbEntity.setContentType("image/jpeg");
      thumbEntity.setRenditionType(RenditionType.THUMBNAIL);
      thumbEntity.setParentUploadId(originalEntity.getId());

      uploadEntityRepository.save(thumbEntity);

      // 7) success
      return UploadFileResultResponse.builder()
          .clientId(clientId)
          .originalFileName(originalName)
          .fileSize(originalEntity.getFileSize())
          .mimeType(optimizedType)
          .status(UploadStatus.SUCCESS)
          .entity(uploadEntityMapper.toDto(originalEntity))
          .build();

    } catch (InvalidFileException ex) {

      return UploadFileResultResponse.builder()
          .clientId(clientId)
          .originalFileName(origiName)
          .status(UploadStatus.FAILED)
          .errorCode(ex.getMessageKey())
          .message(ex.getMessage())
          .build();

    } catch (IOException ex) {

      return UploadFileResultResponse.builder()
          .clientId(clientId)
          .originalFileName(origiName)
          .status(UploadStatus.FAILED)
          .errorCode("IO_ERROR")
          .message("Failed to store file")
          .build();
    }
  }


  @Transactional
  public UploadFileResultResponse uploadSingleFileWithNextVersion(
      MultipartFile file,
      DomainType domainType,
      UploadType uploadType,
      Long ownerId,
      String clientId
  ) {
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        int version = uploadEntityRepository.findMaxVersion(domainType, uploadType, ownerId) + 1;
        return uploadSingleFile(file, domainType, uploadType, ownerId, clientId, version);
      } catch (DataIntegrityViolationException ex) {
        if (attempt == 3) {
          throw ex;
        }
      }
    }
    throw new IllegalStateException("Failed to allocate version");
  }

  public UploadBatchResponse uploadMultipleFiles(List<MultipartFile> files,
      List<String> clientIds,
      DomainType domainType,
      UploadType uploadType,
      Long ownerId) {
    List<UploadFileResultResponse> results = new ArrayList<>();
    List<String> ids = clientIds == null ? List.of() : clientIds;
    boolean hasIds = !ids.isEmpty();

    if (hasIds && ids.size() != files.size()) {
      throw new InvalidRequestException(DmErrorCode.INVALID_MULTIPART_CLIENT_IDS);
    }

    int nextVersion = uploadEntityRepository.findMaxVersion(domainType, uploadType, ownerId) + 1;

    log.info("nextVersion :{}", nextVersion);

    for (int i = 0; i < files.size(); i++) {
      String clientId = hasIds ? ids.get(i) : UUID.randomUUID().toString();
      MultipartFile file = files.get(i);
      int version = nextVersion++;

      log.info("version :{}", version);

      try {
        UploadFileResultResponse ok = uploadSingleFile(file, domainType, uploadType, ownerId,
            clientId,
            version);
        ok.setClientId(clientId);
        results.add(ok);

      } catch (InvalidFileException ex) {
        results.add(
            uploadEntityMapper.toFailedUploadFileResultDTO(file, clientId, ex.getMessageKey(),
                ex.getMessage()));
      } catch (Exception ex) {
        results.add(uploadEntityMapper.toFailedUploadFileResultDTO(file, clientId, "UPLOAD_FAILED",
            "Unexpected upload error"));
      }

    }

    long ok = results.stream().filter(r -> r.getStatus() == UploadStatus.SUCCESS).count();
    long bad = results.size() - ok;

    return UploadBatchResponse.builder()
        .itemId(ownerId)
        .domainType(domainType)
        .uploadType(uploadType)
        .received(results.size())
        .successCount((int) ok)
        .failedCount((int) bad)
        .results(results)
        .build();

  }


  @Transactional
  public Boolean softDeleteDocument(String uploadEntityId) {
    logger.info("Attempting to soft delete document with ID: {}", uploadEntityId);
    try {
      UploadEntity uploadEntity = findUploadEntityId(uploadEntityId);
      FileMeta meta = fileMetaResolver.resolve(uploadEntityId);
      storageService.moveFileToTrash(meta.directory(), meta.filename(),
          uploadEntityId);
      uploadEntity.setDeleted(true);
      uploadEntityRepository.save(uploadEntity);

      List<UploadEntity> children = uploadEntityRepository.findByParentUploadId(uploadEntityId);
      for (UploadEntity child : children) {
        FileMeta cm = fileMetaResolver.resolve(child.getId());
        storageService.moveFileToTrash(cm.directory(), cm.filename(), child.getId());
        child.setDeleted(true);
        uploadEntityRepository.save(child);
      }

      logger.info("Document with ID: {} soft deleted successfully", uploadEntityId);
      return true;
    } catch (IOException e) {
      throw new StorageException("Error while soft deleting document " + uploadEntityId, e);
    }

  }

  private UploadEntity findUploadEntityId(String documentId) throws StorageException {
    return uploadEntityRepository.findById(documentId)
        .orElseThrow(() -> new StorageException(
            "Document with Id: " + documentId + " not found"));

  }

  private UploadEntity findDocumentBy(DomainType domainType, UploadType uploadType, Long ownerId,
      Integer version) throws StorageException {
    return uploadEntityRepository.findByDomainTypeAndUploadTypeAndItemIdAndVersion(domainType,
            uploadType, ownerId, version)
        .orElseThrow(() -> new StorageException(
            "Upload with ownerId: " + ownerId + " and version nr: " + version + " + not found"));

  }

  @Transactional
  public UploadEntityResponse replaceDocument(String uploadId,
      MultipartFile file) throws IOException {
    UploadEntity existing = findUploadEntityId(uploadId);

    validateReplacePermission(existing);

    FileValidationService.ValidatedUpload v = fileValidationService.validateAndRead(file,
        existing.getUploadType(), existing.getItemId());

    byte[] inputBytes = v.bytes();
    String detectedType = v.detectedMimeType();
    String safeName = v.safeFileName();
    String originalName = v.originalFileName();

    String oldPath = existing.getFilePath();
    String oldName = existing.getFileName();

    int newVersion = existing.getVersion() + 1;

    StorageCommandRequest cmd = new StorageCommandRequest(existing.getDomainType(),
        existing.getUploadType(),
        existing.getItemId(), newVersion, originalName, detectedType);

    StoredObjectResponse newObj = storageService.store(file.getInputStream(), cmd);
    Dimension dimension = dmUtils.readImageSize(inputBytes);

    existing.setFileName(newObj.filename());
    existing.setOriginalFileName(newObj.originalFilename());
    existing.setMimeType(detectedType);
    existing.setFileSize(file.getSize());
    existing.setWidth(dimension.width);
    existing.setHeight(dimension.height);
    existing.setVersion(newVersion);
    existing.setDeleted(false);

    UploadEntity saved = uploadEntityRepository.save(existing);

    // Move OLD file to trash
    storageService.moveFileToTrash(oldPath, oldName, existing.getId());

    // Also: regenerate thumbnail if image + delete old thumb if exists
    if (thumbnailService.isImage(file.getContentType())) {
      replaceThumbnailFor(saved, file);
    } else {
      deleteThumbnailFor(saved.getId());
    }

    return uploadEntityMapper.toDto(saved);
  }

  public int getVersion(DomainType domainType, UploadType uploadType, Long ownerId) {

    int version = uploadEntityRepository.findMaxVersion(domainType, uploadType, ownerId) + 1;
    return version;
  }

  /**************************** Private Section ************************************/

  private void validateReplacePermission(UploadEntity existing) {
    RequestContextData ctx = RequestContext.get();

    Long currentUserId = ctx.userId();
    Set<String> roles = ctx.roles();

    boolean isOwner = existing.getItemId() != null
        && existing.getItemId().equals(currentUserId);

    boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
    boolean isModerator = roles != null && roles.contains("ROLE_MODERATOR");

    if (!isOwner && !isAdmin && !isModerator) {
      throw new AccessDeniedException("You are not allowed to replace this image");
    }
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

  private void replaceThumbnailFor(UploadEntity saved, MultipartFile file) throws IOException {
    UploadType thumbType = dmUtils.mapToThumbType(saved.getUploadType());
    if (thumbType == saved.getUploadType()) {
      return;
    }

    storageService.deleteThumbnail(saved.getFilePath(), saved.getFileName());
    storageService.generateThumbnail(saved.getFilePath(), saved.getFileName(),
        file.getContentType());
  }

  private UploadFileResultResponse failed(MultipartFile file, String code, String message) {
    return UploadFileResultResponse.builder()
        .originalFileName(file != null ? file.getOriginalFilename() : null)
        .fileSize(file != null ? file.getSize() : null)
        .mimeType(file != null ? file.getContentType() : null)
        .status(UploadStatus.FAILED)
        .errorCode(code)
        .message(message)
        .build();
  }


}
