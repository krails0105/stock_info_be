package io.github.krails0105.stock_info_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.krails0105.stock_info_api.dto.IndexDto;
import io.github.krails0105.stock_info_api.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class IndexController {

  private final IndexService indexService;

  @GetMapping("/indices")
  public List<IndexDto> getIndices() {
    log.info("Request to get indices");
    return indexService.getIndices();
  }
}
