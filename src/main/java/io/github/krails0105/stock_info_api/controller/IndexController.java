package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.domain.Index;
import io.github.krails0105.stock_info_api.dto.response.IndexResponse;
import io.github.krails0105.stock_info_api.service.IndexService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indexes")
@RequiredArgsConstructor
@Slf4j
public class IndexController {

  private final IndexService indexService;

  @GetMapping
  public List<IndexResponse> getIndexes() {
    List<Index> indexes = indexService.getIndexes();
    return indexes.stream().map(IndexResponse::fromIndex).toList();
  }
}
