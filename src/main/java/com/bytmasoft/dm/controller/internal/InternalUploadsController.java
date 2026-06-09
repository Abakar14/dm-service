package com.bytmasoft.dm.controller.internal;

import com.bytmasoft.dm.dto.page.PagedResponse;
import com.bytmasoft.dm.dto.response.UploadBatchResponse;
import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import com.bytmasoft.dm.dto.response.UploadFileResultResponse;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.exception.StorageException;
import com.bytmasoft.dm.service.InternalUploadsStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Internal write endpoints intended to be called only via Gateway/BFF.
 * <p>
 * Pair with InternalRequestSecretFilter (dm.security.internal-shared-secret) for defense-in-depth.
 */

@Slf4j
@Tag(name = "Internal Document")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/internal/dm/{entityType}", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalUploadsController {

  private final InternalUploadsStorageService storageService;

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("addedOn", "modifiedOn", "version");
  private static final Set<String> VALID_ENTITY_TYPES = Set.of("images", "documents");

  // ---------------------------- Helper to validate entityType ----------------------------
  private void validateEntityType(String entityType) {
    if (!VALID_ENTITY_TYPES.contains(entityType)) {
      throw new IllegalArgumentException("Invalid entityType. Must be 'images' or 'documents'");
    }
  }

  // ---------------------------- Common Endpoints (merged) ----------------------------

  @GetMapping("/owners")
  public ResponseEntity<Page<UploadEntityResponse>> getAllByOwners(
      @PathVariable String entityType,
      @RequestParam @NotNull DomainType domainType,
      @RequestParam(required = false) UploadType uploadType,
      @RequestParam @NotNull List<@Positive Long> ownerIds,
      @RequestParam(required = false) @Positive Integer version,
      @PageableDefault(page = 0, size = 10, sort = {"id",
          "fileName"}, direction = Sort.Direction.ASC) Pageable pageable) {
    validateEntityType(entityType);
    return ResponseEntity.ok(
        storageService.getAllDocumentsOwners(domainType, uploadType, ownerIds, version,
            sanitize(pageable)));
  }

  @GetMapping("/{uploadId}/download")
  public ResponseEntity<Resource> downloadById(@PathVariable String entityType,
      @PathVariable String uploadId) {
    validateEntityType(entityType);
    try {
      Resource resource = storageService.downloadDocumentById(uploadId);
      return buildDownloadResponse(resource);
    } catch (StorageException e) {
      log.warn("Document not found: uploadId={}", uploadId, e);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error downloading document: uploadId={}", uploadId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/download")
  public ResponseEntity<Resource> downloadByKey(
      @PathVariable String entityType,
      @RequestParam @NotNull DomainType domainType,
      @RequestParam @NotNull UploadType uploadType,
      @RequestParam @Positive Long ownerId,
      @RequestParam @Positive Integer version) {
    validateEntityType(entityType);
    try {
      Resource file = storageService.downloadDocument(domainType, uploadType, ownerId, version);
      return buildDownloadResponse(file);
    } catch (StorageException e) {
      log.warn("Document not found: domainType={}, uploadType={}, ownerId={}, version={}",
          domainType, uploadType, ownerId, version, e);
      return ResponseEntity.notFound().build();
    } catch (IOException e) {
      log.error("IO error downloading document", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(value = "/downloads", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> downloadAllAsZip(
      @PathVariable String entityType,
      @RequestParam @NotNull DomainType domainType,
      @RequestParam(required = false) UploadType uploadType,
      @RequestParam(required = false) @Positive Long ownerId,
      @RequestParam(required = false) @Positive Integer version) {
    validateEntityType(entityType);
    try {
      ByteArrayResource zip = storageService.downloadAllDocumentsAsZip(domainType, uploadType,
          ownerId, version);
      return buildZipDownloadResponse(zip);
    } catch (StorageException e) {
      log.warn("No documents found to zip", e);
      return ResponseEntity.notFound().build();
    } catch (IOException e) {
      log.error("Error creating zip", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadFileResultResponse> uploadSingle(
      @PathVariable String entityType,
      @RequestParam("file") @Valid MultipartFile file,
      @RequestParam(value = "clientId", required = false) String clientId,
      @RequestParam @NotNull DomainType domainType,
      @RequestParam @NotNull UploadType uploadType,
      @RequestParam @Positive Long ownerId) {
    validateEntityType(entityType);
    UploadFileResultResponse result = storageService.uploadSingleFileWithNextVersion(file,
        domainType,
        uploadType,
        ownerId, clientId);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadBatchResponse> uploadMultiple(
      @PathVariable String entityType,
      @RequestPart("files") @NotNull List<MultipartFile> files,
      @RequestParam(value = "clientIds", required = false) List<String> clientIds,
      @RequestParam @NotNull DomainType domainType,
      @RequestParam @NotNull UploadType uploadType,
      @RequestParam @Positive Long ownerId) {
    validateEntityType(entityType);
    UploadBatchResponse result = storageService.uploadMultipleFiles(files, clientIds, domainType,
        uploadType, ownerId);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PutMapping(value = "/{uploadId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadEntityResponse> replace(
      @PathVariable String entityType,
      @PathVariable String uploadId,
      @RequestParam("file") @Valid MultipartFile file) {
    validateEntityType(entityType);
    try {
      UploadEntityResponse updated = storageService.replaceDocument(uploadId, file);
      return ResponseEntity.ok(updated);
    } catch (StorageException e) {
      log.warn("Document not found for replacement: uploadId={}", uploadId, e);
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error replacing document: uploadId={}", uploadId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @DeleteMapping("/{uploadId}")
  public ResponseEntity<Boolean> softDelete(@PathVariable String entityType,
      @PathVariable String uploadId) {
    validateEntityType(entityType);
    try {
      Boolean deleted = storageService.softDeleteDocument(uploadId);
      return ResponseEntity.ok(deleted);
    } catch (StorageException e) {
      log.warn("Document not found for deletion: uploadId={}", uploadId, e);
      return ResponseEntity.notFound().build();
    }
  }

  // ---------------------------- Additional endpoints ----------------------------
  // The /{uploadId} GET (get metadata) – only for documents, but we keep it unified
  @GetMapping("/{uploadId}")
  public ResponseEntity<UploadEntityResponse> getUploadById(@PathVariable String entityType,
      @PathVariable String uploadId) {
    validateEntityType(entityType);
    try {
      UploadEntityResponse dto = storageService.getUploadsById(uploadId);
      return ResponseEntity.ok(dto);
    } catch (StorageException e) {
      log.warn("Document metadata not found: uploadId={}", uploadId, e);
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("")
  public PagedResponse<UploadEntityResponse> getAll(
      @PathVariable String entityType,
      @RequestParam @NotNull DomainType domainType,
      @RequestParam(required = false) UploadType uploadType,
      @RequestParam(required = false) @Positive Long ownerId,
      Pageable pageable) {
    validateEntityType(entityType);
    return storageService.getAllDocuments(domainType, uploadType, ownerId, sanitize(pageable));
  }

  // ---------------------------- Private Helpers ----------------------------

  private Pageable sanitize(Pageable pageable) {
    Sort sort = pageable.getSort().stream()
        .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
        .collect(Collectors.collectingAndThen(Collectors.toList(), Sort::by));
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }

  private ResponseEntity<Resource> buildDownloadResponse(Resource resource) throws IOException {
    String contentType = null;
    try {
      contentType = Files.probeContentType(Paths.get(resource.getURI()));
    } catch (Exception ignored) {
      log.debug("Could not determine content type for {}", resource.getFilename());
    }
    MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType)
        : MediaType.APPLICATION_OCTET_STREAM;
    String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8)
        .replaceAll("\\+", "%20");

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
        .body(resource);
  }

  private ResponseEntity<Resource> buildZipDownloadResponse(ByteArrayResource zipResource) {
    String encodedFilename = URLEncoder.encode(zipResource.getFilename(), StandardCharsets.UTF_8)
        .replaceAll("\\+", "%20");
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
        .body(zipResource);
  }
}