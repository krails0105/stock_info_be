package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.StockDto;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

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
  public StockDto getStock(String code) {
    log.info("Getting stock with code: {}", code);

    // 전체 데이터에서 해당 종목 코드 찾기
    return getStockData(null).stream()
        .filter(stock -> stock.getStockCode().equals(code))
        .findFirst()
        .orElse(null);
  }

  public List<StockDto> searchStocks(String keyword) {
    log.info("Searching stocks with keyword: {}", keyword);

    // 임시 검색 로직 (나중에 구현)
    return getStockData(null).stream()
        .filter(
            stock ->
                stock.getStockName().contains(keyword) || stock.getStockCode().contains(keyword))
        .collect(Collectors.toList());
  }

  private List<StockDto> getStockData(String sector) {
    // 임시 데이터 반환
    if ("tech".equals(sector)) {
      return Arrays.asList(
          new StockDto(
              "005930",
              "삼성전자",
              "tech",
              "75,000",
              "+1,000",
              "+1.35%",
              "up",
              "15,234,567",
              "15.2",
              "1.8",
              "450조",
              "2.1%",
              "2024-01-15 15:30:00"),
          new StockDto(
              "000660",
              "SK하이닉스",
              "tech",
              "125,000",
              "-2,500",
              "-1.96%",
              "down",
              "8,765,432",
              "12.8",
              "2.1",
              "90조",
              "1.5%",
              "2024-01-15 15:30:00"),
          new StockDto(
              "035720",
              "카카오",
              "tech",
              "55,000",
              "+500",
              "+0.92%",
              "up",
              "3,456,789",
              "18.5",
              "1.2",
              "25조",
              "0.8%",
              "2024-01-15 15:30:00"));
    } else if ("bio".equals(sector)) {
      return Arrays.asList(
          new StockDto(
              "207940",
              "삼성바이오로직스",
              "bio",
              "850,000",
              "+15,000",
              "+1.80%",
              "up",
              "1,234,567",
              "28.5",
              "3.2",
              "70조",
              "0.5%",
              "2024-01-15 15:30:00"),
          new StockDto(
              "068270",
              "셀트리온",
              "bio",
              "195,000",
              "-3,000",
              "-1.51%",
              "down",
              "2,345,678",
              "22.1",
              "2.8",
              "25조",
              "1.2%",
              "2024-01-15 15:30:00"));
    } else {
      // 전체 종목 또는 기본 데이터
      return Arrays.asList(
          new StockDto(
              "005930",
              "삼성전자",
              "tech",
              "75,000",
              "+1,000",
              "+1.35%",
              "up",
              "15,234,567",
              "15.2",
              "1.8",
              "450조",
              "2.1%",
              "2024-01-15 15:30:00"),
          new StockDto(
              "000660",
              "SK하이닉스",
              "tech",
              "125,000",
              "-2,500",
              "-1.96%",
              "down",
              "8,765,432",
              "12.8",
              "2.1",
              "90조",
              "1.5%",
              "2024-01-15 15:30:00"),
          new StockDto(
              "207940",
              "삼성바이오로직스",
              "bio",
              "850,000",
              "+15,000",
              "+1.80%",
              "up",
              "1,234,567",
              "28.5",
              "3.2",
              "70조",
              "0.5%",
              "2024-01-15 15:30:00"),
          new StockDto(
              "068270",
              "셀트리온",
              "bio",
              "195,000",
              "-3,000",
              "-1.51%",
              "down",
              "2,345,678",
              "22.1",
              "2.8",
              "25조",
              "1.2%",
              "2024-01-15 15:30:00"));
    }
  }
}
