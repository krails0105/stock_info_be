package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.StockListResponse;
import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.response.ChartResponse;
import io.github.krails0105.stock_info_api.dto.response.StockResponse;
import io.github.krails0105.stock_info_api.service.StockService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
public class StockController {

  private final StockService stockService;

  /** 섹터별 종목 리스트 (점수순 정렬) */
  @GetMapping("/sector/{sectorId}")
  public ResponseEntity<StockListResponse> getStocksBySector(@PathVariable String sectorId) {
    log.info("Request to get stocks by sector: {}", sectorId);
    StockListResponse response = stockService.getStocksBySector(sectorId);
    if (response == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(response);
  }

  /** 종목 상세 조회 */
  @GetMapping("/{id}")
  public ResponseEntity<StockResponse> getStockById(@PathVariable String id) {
    log.info("Request to get stock: {}", id);
    StockInfo stockInfo = stockService.getStockById(id);
    if (stockInfo == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(StockResponse.fromStockInfo(stockInfo));
  }

  /** 종목 검색 */
  @GetMapping("/search")
  public List<StockScoreDto> searchStocks(@RequestParam String keyword) {
    log.info("Request to search stocks: {}", keyword);
    return stockService.searchStocks(keyword);
  }

  /** 전체 상위 종목 (점수순) */
  @GetMapping("/top")
  public List<StockScoreDto> getTopStocks(@RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get top {} stocks", limit);
    return stockService.getTopStocks(limit);
  }

  /** 종목 차트 데이터 조회 */
  @GetMapping("/{code}/chart")
  public ResponseEntity<ChartResponse> getStockChart(
      @PathVariable String code, @RequestParam(defaultValue = "1M") String range) {
    log.info("Request to get chart for stock: {}, range: {}", code, range);
    ChartResponse response = stockService.getStockChart(code, range);
    return ResponseEntity.ok(response);
  }
}
