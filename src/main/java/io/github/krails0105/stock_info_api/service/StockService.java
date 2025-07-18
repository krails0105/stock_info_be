package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.NewsDto;
import io.github.krails0105.stock_info_api.dto.StockDetailDto;
import io.github.krails0105.stock_info_api.dto.StockDto;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

  private static final Map<String, List<StockDto>> STOCK_DATA = Map.of(
      "기술주", List.of(
          new StockDto("005930", "삼성전자", 75000, 15.2, 1.8),
          new StockDto("000660", "SK하이닉스", 125000, 12.8, 2.1),
          new StockDto("035720", "카카오", 55000, 18.5, 1.2)
      )
  );

  // 여러 종목 조회 (List 사용)
  public List<StockDto> getStocks(String sector, String sortBy, String sortOrder) {
    log.info("Getting stocks for sector: {}, sortBy: {}, sortOrder: {}", sector, sortBy, sortOrder);

    // 임시 데이터 반환 (나중에 외부 API 연동으로 대체)
    List<StockDto> stocks = getStockData(sector);

    // 정렬 로직 (나중에 구현)
    if (sortBy != null && !sortBy.isEmpty()) {
      // TODO: 정렬 로직 구현
      log.info("Sorting by: {} in order: {}", sortBy, sortOrder);
    }

    return stocks;
  }

  // 단일 종목 조회 (일반 객체 사용)
  public StockDetailDto getStock(String code) {
    log.info("Getting stock with code: {}", code);

    // 전체 데이터에서 해당 종목 코드 찾기
    return getStockData(null).stream()
        .filter(stock -> stock.getCode().equals(code))
        .findFirst()
        .map(
            stock -> {
              long volume = (long) (Math.random() * 1000000000);

              List<NewsDto> news =
                  List.of(
                      new NewsDto(stock.getName() + " 뉴스 1", "contents1"),
                      new NewsDto(stock.getName() + " 뉴스 2", "contents2"));
              return new StockDetailDto(
                  stock.getCode(),
                  stock.getName(),
                  stock.getPrice(),
                  stock.getPer(),
                  stock.getPbr(),
                  volume,
                  news);
            })
        .orElse(null);
  }

  public List<StockDto> searchStocks(String keyword) {
    log.info("Searching stocks with keyword: {}", keyword);

    // 임시 검색 로직 (나중에 구현)
    return getStockData(null).stream()
        .filter(stock -> stock.getName().contains(keyword) || stock.getCode().contains(keyword))
        .collect(Collectors.toList());
  }

  private List<StockDto> getStockData(String sector) {
    if (sector == null) {
      return STOCK_DATA.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
    log.info("Getting stock data for sector: {}", sector);
    if (!STOCK_DATA.containsKey(sector)) {
      log.info("Sector not found: {}", sector);
      return List.of();
    }
    return STOCK_DATA.get(sector);
  }
}
