package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.StockDetailDto;
import io.github.krails0105.stock_info_api.dto.StockDto;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class StockController {

  private final StockService stockService;

  // 여러 종목 조회 (List 사용)
  @GetMapping("/stocks")
  public List<StockDto> getStocks(
      @RequestParam(required = false) String sector,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortOrder) {
    log.info(
        "Request to get stocks - sector: {}, sortBy: {}, sortOrder: {}", sector, sortBy, sortOrder);
    return stockService.getStocks(sector, sortBy, sortOrder);
  }

  // 단일 종목 조회 (ResponseEntity 사용)
  @GetMapping("/stock/{code}")
  public ResponseEntity<StockDetailDto> getStock(@PathVariable String code) {
    log.info("Request to get stock with code: {}", code);
    StockDetailDto stock = stockService.getStock(code);
    if (stock != null) {
      return ResponseEntity.ok(stock);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/search")
  public List<StockDto> searchStocks(@RequestParam String keyword) {
    log.info("Request to search stocks with keyword: {}", keyword);
    return stockService.searchStocks(keyword);
  }
}
