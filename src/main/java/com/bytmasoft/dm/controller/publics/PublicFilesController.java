package com.bytmasoft.dm.controller.publics;


import com.bytmasoft.dm.dto.response.StoredFileResourceResponse;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.service.PublicFileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/public/dm/files", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicFilesController {

  @Value("${dm.security.public-downloads-enabled}")
  private boolean publicDownloadsEnabled;

  private final PublicFileService publicFileService;


  @GetMapping(value = "/{fileId}")
  public ResponseEntity<Resource> download(@PathVariable String fileId) throws IOException {
    if (!publicDownloadsEnabled) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    StoredFileResourceResponse sfr = publicFileService.load(fileId); // or download()
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(sfr.contentType()))
        .header(HttpHeaders.ETAG, sfr.etag())
        .body(sfr.resource());
  }

  @GetMapping(value = "/{fileId}/thumb", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<Resource> downloadThumbnail(@PathVariable String fileId)
      throws IOException {
    if (!publicDownloadsEnabled) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    StoredFileResourceResponse sfr = publicFileService.loadThumbnail(fileId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(sfr.contentType()))
        .header(HttpHeaders.ETAG, sfr.etag())
        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
        .body(sfr.resource());
  }


  @GetMapping("/download")
  public ResponseEntity<Resource> downloadCoverImage(
      @RequestParam DomainType domainType,
      @RequestParam UploadType uploadType,
      @RequestParam Long ownerId,
      @RequestParam Integer version) throws IOException {
    if (!publicDownloadsEnabled) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    Resource file = publicFileService.downloadDocument(domainType, uploadType,
        ownerId, version);
    String fileContentType = Files.probeContentType(Paths.get(file.getURI()));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(fileContentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename())
        .body(file);

  }


  @GetMapping("/{uploadId}/original-url")
  public String getOriginalUrl(@PathVariable Long uploadId) {
    return publicFileService.buildOriginalUrl(uploadId);
  }

  @GetMapping("/{uploadId}/thumb-url")
  public String getThumUrl(@PathVariable Long uploadId) {
    return publicFileService.buildThumbUrl(uploadId);
  }


}
