package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.insight.SectorInsight;
import io.github.krails0105.stock_info_api.dto.insight.StockInsight;
import io.github.krails0105.stock_info_api.service.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인사이트 API 컨트롤러. 종목/섹터에 대한 10초 요약, 룰 기반 분석 결과를 제공.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InsightController {

  private final InsightService insightService;

  /**
   * 종목 인사이트 조회
   *
   * @param stockCode 종목 코드 (예: "005930")
   * @return StockInsight - 10초 요약, 긍정/주의 카드, 뉴스 등
   */
  @GetMapping("/stocks/{stockCode}/insight")
  public ResponseEntity<StockInsight> getStockInsight(@PathVariable String stockCode) {
    log.info("종목 인사이트 조회 요청: {}", stockCode);
    StockInsight insight = insightService.getStockInsight(stockCode);
    return ResponseEntity.ok(insight);
  }

  /**
   * 섹터 인사이트 조회
   *
   * @param sectorName 섹터명 (예: "전기전자")
   * @return SectorInsight - 섹터 브리핑, Top Picks, 뉴스 등
   */
  @GetMapping("/sectors/{sectorName}/insight")
  public ResponseEntity<SectorInsight> getSectorInsight(@PathVariable String sectorName) {
    log.info("섹터 인사이트 조회 요청: {}", sectorName);
    SectorInsight insight = insightService.getSectorInsight(sectorName);
    return ResponseEntity.ok(insight);
  }
}
