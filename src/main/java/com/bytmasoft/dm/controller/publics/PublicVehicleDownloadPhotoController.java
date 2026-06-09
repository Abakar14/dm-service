package com.bytmasoft.dm.controller.publics;

import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import com.bytmasoft.dm.exception.StorageException;
import com.bytmasoft.dm.service.InternalUploadsStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PublicVehicleController
 *
 * @author Mahamat Abakar
 * @since 02.05.26
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/public/dm/media", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicVehicleDownloadPhotoController {

  private final InternalUploadsStorageService storageService;

  @GetMapping("/{uploadId}")
  public ResponseEntity<UploadEntityResponse> getUploadById(@PathVariable String entityType,
      @PathVariable String uploadId) {
    try {
      UploadEntityResponse dto = storageService.getUploadsById(uploadId);
      return ResponseEntity.ok(dto);
    } catch (StorageException e) {
      log.warn("Document metadata not found: uploadId={}", uploadId, e);
      return ResponseEntity.notFound().build();
    }
  }


}
