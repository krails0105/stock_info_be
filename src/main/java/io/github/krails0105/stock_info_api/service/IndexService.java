package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.IndexDto;
import java.util.Arrays;
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

    // 임시 데이터 반환 (나중에 외부 API 연동으로 대체)
    return Arrays.asList(
        new IndexDto("KOSPI", "2,450.30", "+15.20", "+0.62%", "up", "2024-01-15 15:30:00"),
        new IndexDto("KOSDAQ", "850.45", "-5.80", "-0.68%", "down", "2024-01-15 15:30:00"));
  }
}
