package com.bytmasoft.dm.controller.publics;


import com.bytmasoft.dm.dto.request.CoverBatchRequest;
import com.bytmasoft.dm.dto.response.CoverBatchResponse;
import com.bytmasoft.dm.dto.response.ImageRefResponse;
import com.bytmasoft.dm.enums.UploadType;
import com.bytmasoft.dm.service.ImageQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/public/dm/images", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicItemImagesController {

  private final ImageQueryService queryService;

  @GetMapping(value = "/items/{itemId}/found")
  public List<ImageRefResponse> getFoundItemImages(
      @PathVariable Long itemId
  ) {
    return queryService.listImages(itemId, UploadType.FOUND_ITEM_IMAGE);
  }

  @GetMapping(value = "/items/{itemId}/lost")
  public List<ImageRefResponse> getLostItemImages(
      @PathVariable Long itemId
  ) {
    return queryService.listImages(itemId, UploadType.LOST_ITEM_IMAGE);
  }

  @PostMapping(value = "/items/covers")
  public CoverBatchResponse covers(@RequestBody CoverBatchRequest req) {
    if (req.uploadType() == null) {
      return queryService.getCovers(req.ownerIds());

    } else {
      return queryService.getCovers(req.uploadType(), req.ownerIds());

    }
  }


}
