package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.ScoreboardResponse;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.response.StockListItem;
import io.github.krails0105.stock_info_api.dto.response.StockResponse;
import io.github.krails0105.stock_info_api.service.SectorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
@Slf4j
public class SectorController {

  private final SectorService sectorService;

  /** 홈 현황판 API - Hot Sectors TOP3 + 전체 섹터 리스트 + 시장 요약 */
  @GetMapping("/scoreboard")
  public ScoreboardResponse getScoreboard() {
    log.info("Request to get scoreboard");
    return sectorService.getScoreboard();
  }

  /** 전체 섹터 리스트 (점수순 정렬) */
  @GetMapping
  public List<SectorScoreDto> getAllSectors() {
    log.info("Request to get all sectors");
    return sectorService.getAllSectors();
  }

  /** 특정 섹터의 종목 목록 조회 - Controller에서 Response DTO로 변환 */
  @GetMapping("/{sectorId}")
  public ResponseEntity<List<StockResponse>> getStocksBySectorId(@PathVariable String sectorId) {
    log.info("Request to get sector stocks: {}", sectorId);

    // Service에서 Domain DTO 조회
    List<StockInfo> stocks = sectorService.getStocksBySectorId(sectorId);
    if (stocks.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // Controller에서 Response DTO로 변환
    List<StockResponse> response = stocks.stream().map(StockResponse::fromStockInfo).toList();
    return ResponseEntity.ok(response);
  }

  /**
   * 섹터별 종목 목록 조회
   *
   * @param sectorName 업종명 (예: "전기전자", "바이오")
   * @return 해당 업종에 속한 종목 목록
   */
  @GetMapping("/{sectorName}/stocks")
  public ResponseEntity<List<StockListItem>> getStocksBySectorName(
      @PathVariable String sectorName) {
    log.info("Request to get stocks by sector name: {}", sectorName);
    List<StockListItem> stocks = sectorService.getStocksBySectorName(sectorName);
    if (stocks.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(stocks);
  }
}
