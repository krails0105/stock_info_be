package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.response.HomePicksResponse;
import io.github.krails0105.stock_info_api.service.HomePicksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 홈 페이지 API 컨트롤러 */
@Slf4j
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

  private final HomePicksService homePicksService;

  /**
   * 홈 Watchlist Picks 조회
   *
   * @param size 선정할 종목 수 (기본 8, 최대 10)
   * @param preset 프리셋 (default/stable/momentum/value)
   * @return HomePicksResponse
   */
  @GetMapping("/picks")
  public HomePicksResponse getHomePicks(
      @RequestParam(defaultValue = "8") int size,
      @RequestParam(defaultValue = "default") String preset) {
    log.info("Request home picks: size={}, preset={}", size, preset);

    // size 제한 (5~10)
    int validSize = Math.max(5, Math.min(10, size));

    return homePicksService.getHomePicks(validSize, preset);
  }
}
