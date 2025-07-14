package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.IndexDto;
import io.github.krails0105.stock_info_api.service.IndexService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indices")
@RequiredArgsConstructor
@Slf4j
public class IndexController {

  private final IndexService indexService;

  @GetMapping
  public List<IndexDto> getIndices() {
    log.info("Request to get indices");
    return indexService.getIndices();
  }
}
