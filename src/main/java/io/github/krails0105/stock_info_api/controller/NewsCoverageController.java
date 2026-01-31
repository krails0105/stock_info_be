package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.response.NewsCoverageResponse;
import io.github.krails0105.stock_info_api.service.NewsCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 뉴스 커버리지 API 컨트롤러.
 *
 * <p>운영 관점에서 뉴스 커버리지 현황을 모니터링할 수 있는 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsCoverageController {

  private final NewsCoverageService newsCoverageService;

  /**
   * 뉴스 커버리지 현황 조회.
   *
   * <p>종목/섹터별 뉴스 커버리지 지표와 "뉴스 0건" 목록을 반환한다.
   *
   * @return 커버리지 현황
   */
  @GetMapping("/coverage")
  public ResponseEntity<NewsCoverageResponse> getCoverage() {
    NewsCoverageResponse response = newsCoverageService.getCoverageReport();
    return ResponseEntity.ok(response);
  }
}
