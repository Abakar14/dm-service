package com.bytmasoft.dm.controller.admin;


import com.bytmasoft.dm.dto.page.PagedResponse;
import com.bytmasoft.dm.dto.response.TrashPurgeResponse;
import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.service.AdminUploadsStorageService;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/admin/dm//images", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminUploadsController {

  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("addedOn", "modifiedOn", "version");
  private final AdminUploadsStorageService uploadsStorageService;

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public PagedResponse<UploadEntityResponse> getDocuments(
      @RequestParam DomainType domainType,
      @RequestParam(required = false) UploadType uploadType,
      @RequestParam(required = false) Long ownerId,
      Pageable pageable) {
    return uploadsStorageService.getAllDocuments(domainType, uploadType, ownerId,
        sanitize(pageable));

  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{uploadId}/permanent")
  public ResponseEntity<Boolean> deleteDocument(@Valid @PathVariable("uploadId") String uploadId)
      throws Exception {
    return ResponseEntity.ok(uploadsStorageService.permanentlyDelete(uploadId));
  }

  //Admin delete all Trash (bulk purge)
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/trash")
  public ResponseEntity<TrashPurgeResponse> deleteAll(
      @RequestParam("domainType") DomainType domainType,
      @RequestParam(defaultValue = "14") int olderThanDays)
      throws Exception {

    return ResponseEntity.ok(uploadsStorageService.purgeTrash(domainType, olderThanDays));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{uploadId}/restore")
  public ResponseEntity<UploadEntityResponse> deleteAll(
      @PathVariable String uploadId) {
    return ResponseEntity.ok(uploadsStorageService.restoreFile(uploadId));
  }

  private Pageable sanitize(Pageable pageable) {
    Sort sort = pageable.getSort().stream()
        .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            Sort::by
        ));

    return PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        sort
    );
  }
}
