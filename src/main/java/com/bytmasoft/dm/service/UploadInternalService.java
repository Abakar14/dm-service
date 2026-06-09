package com.bytmasoft.dm.service;

import com.bytmasoft.dm.dto.response.ItemUploadPublicInfoResponse;
import com.bytmasoft.dm.entity.UploadEntity;
import com.bytmasoft.dm.enums.DomainType;
import com.bytmasoft.dm.repository.UploadEntityRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UploadInternalService
 *
 * @author Mahamat Abakar
 * @since 04.06.26
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UploadInternalService {


  private final UploadEntityRepository repository;


  @Transactional(readOnly = true)
  public List<ItemUploadPublicInfoResponse> getItemPublicInfo(
      List<Long> itemIds
  ) {

    if (itemIds == null || itemIds.isEmpty()) {
      return List.of();
    }

    List<UploadEntity> uploads = repository.findActiveItemImages(
        itemIds,
        DomainType.ITEM

    );

    Map<Long, UploadEntity> firstUploadByItemId = uploads.stream()
        .filter(u -> u.getItemId() != null)
        .collect(Collectors.toMap(
            UploadEntity::getItemId,
            Function.identity(),
            (existing, replacement) -> existing,
            LinkedHashMap::new
        ));

    return firstUploadByItemId.values()
        .stream()
        .map(this::toItemUploadPublicInfo)
        .toList();
  }

  private ItemUploadPublicInfoResponse toItemUploadPublicInfo(UploadEntity upload) {
    return new ItemUploadPublicInfoResponse(
        upload.getItemId(),
        upload.getId(),
        "/bff/public/dm/files/" + upload.getId(),
        "/bff/public/dm/files/" + upload.getId() + "/thumb",
        "/bff/public/dm/files/" + upload.getId() + "/preview",
        upload.getContentType()
    );
  }
}
