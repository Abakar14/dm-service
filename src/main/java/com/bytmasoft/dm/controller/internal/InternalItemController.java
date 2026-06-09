package com.bytmasoft.dm.controller.internal;

import com.bytmasoft.dm.dto.response.ItemUploadPublicInfoResponse;
import com.bytmasoft.dm.service.UploadInternalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * InternalItemController
 *
 * @author Mahamat Abakar
 * @since 04.06.26
 */
@Slf4j
@Tag(name = "Internal Document")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/dm/uploads")
public class InternalItemController {

  private final UploadInternalService service;

  @PostMapping("/items/public-info")
  public List<ItemUploadPublicInfoResponse> getItemPublicInfo(
      @RequestBody List<Long> itemIds
  ) {
    return service.getItemPublicInfo(itemIds);
  }

}
