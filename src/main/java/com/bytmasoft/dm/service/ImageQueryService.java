package com.bytmasoft.dm.service;

import com.bytmasoft.dm.dto.response.CoverBatchResponse;
import com.bytmasoft.dm.dto.response.CoverResponse;
import com.bytmasoft.dm.dto.response.ImageRefResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.repository.CoverProjection;
import com.bytmasoft.dm.repository.UploadEntityRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageQueryService {

  @Value("${file.storage.download.thumbUrl}")
  private String thumbUrl;

  @Value("${file.storage.download.downloadUrl}")
  private String downloadUrl;


  private final UploadEntityRepository repository;

  public List<ImageRefResponse> listImages(Long itemId, UploadType uploadType) {
    List<UploadEntity> entities = repository.findByDomainTypeAndUploadTypeAndItemId(
        DomainType.ITEM, uploadType, itemId);

    return entities.stream().map(f -> new ImageRefResponse(
        f.getId(),
        f.getFileName(),
        f.getContentType(),
        f.getFileSize(),
        f.getWidth(),
        f.getHeight(),
        this.downloadUrl + "/" + f.getId(),
        this.thumbUrl + "/" + f.getId() + "/thumb"
    )).toList();
  }

  //TODO cover rule in v2: 1. cover = first uploaded image or cover = newest image or cover = image where isCover = true
  public CoverBatchResponse getCovers(UploadType uploadType, List<Long> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return new CoverBatchResponse(Map.of());
    }
    List<CoverProjection> projections = repository.findCoversAndThumbnails(ownerIds,
        uploadType.name());

    Map<Long, CoverResponse> coverDTOMap = projections.stream()
        .collect(Collectors.toMap(
            CoverProjection::getOwnerId,
            proj -> new CoverResponse(
                proj.getOriginalId(),
                proj.getThumbnailId(),
                downloadUrl + "/" + proj.getOriginalId(),
                thumbUrl + "/" + proj.getThumbnailId() + "/thumb",
                Math.toIntExact(proj.getImageCount())
            )
        ));

    return new CoverBatchResponse(coverDTOMap);

  }

  public CoverBatchResponse getCovers(List<Long> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return new CoverBatchResponse(Map.of());
    }
    List<CoverProjection> projections = repository.findCoversAndThumbnails(ownerIds);
    Map<Long, CoverResponse> coverDTOMap = projections.stream()
        .collect(Collectors.toMap(
            CoverProjection::getOwnerId,
            proj -> new CoverResponse(
                proj.getOriginalId(),
                proj.getThumbnailId(),
                downloadUrl + "/" + proj.getOriginalId(),
                thumbUrl + "/" + proj.getThumbnailId() + "/thumb",
                Math.toIntExact(proj.getImageCount())
            )
        ));
    return new CoverBatchResponse(coverDTOMap);
  }

  public record Row(long ownerId, String coverFileId, int imageCount) {

  }
}
