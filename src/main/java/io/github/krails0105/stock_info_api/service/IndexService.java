package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.IndexDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexService {

  public List<IndexDto> getIndices() {
    log.info("Getting indices information");

    return List.of(
        new IndexDto("KOSPI", 2450.30, 15.20, 0.62),
        new IndexDto("KOSDAQ", 850.45, -5.80, -0.68));
  }
}
