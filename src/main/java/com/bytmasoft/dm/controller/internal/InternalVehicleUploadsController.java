package com.bytmasoft.dm.controller.internal;

import com.bytmasoft.dm.dto.response.UploadBatchResponse;
import com.bytmasoft.dm.dto.response.UploadFileResultResponse;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.service.InternalUploadsStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * InternalVehicleUploadsController
 *
 * @author Mahamat Abakar
 * @since 02.05.26 Purpose item-service -> DM-service
 */
@Slf4j
@Tag(name = "Internal Photo")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/internal/dm/vehicles/photos", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalVehicleUploadsController {

  private final InternalUploadsStorageService storageService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadFileResultResponse> uploadSingle(
      @RequestParam("file") @Valid MultipartFile file,
      @RequestParam(value = "clientId", required = false) String clientId,
      @RequestParam @Positive Long ownerId) {
    UploadFileResultResponse result = storageService.uploadSingleFileWithNextVersion(file,
        DomainType.VEHICLE,
        UploadType.VEHICLE_PHOTO,
        ownerId, clientId);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadBatchResponse> uploadMultiple(
      @RequestPart("files") @NotNull List<MultipartFile> files,
      @RequestParam(value = "clientIds", required = false) List<String> clientIds,
      @RequestParam @Positive Long ownerId) {
    UploadBatchResponse result = storageService.uploadMultipleFiles(files, clientIds,
        DomainType.VEHICLE,
        UploadType.VEHICLE_PHOTO, ownerId);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }


}
