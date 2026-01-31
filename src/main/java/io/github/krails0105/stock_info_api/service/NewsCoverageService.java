package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.response.NewsCoverageResponse;
import io.github.krails0105.stock_info_api.dto.response.NewsCoverageResponse.SectorCoverage;
import io.github.krails0105.stock_info_api.dto.response.NewsCoverageResponse.StockCoverage;
import io.github.krails0105.stock_info_api.repository.ProcessedNewsArticleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 뉴스 커버리지 지표 서비스.
 *
 * <p>종목/섹터별 뉴스 커버리지 현황을 계산하고 "뉴스 0건" 구간을 파악할 수 있도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCoverageService {

  private final ProcessedNewsArticleRepository processedNewsRepository;

  /** 모니터링 대상 종목 코드 (시총 상위 40개) */
  private static final Map<String, String> MONITORED_STOCKS =
      Map.ofEntries(
          Map.entry("005930", "삼성전자"),
          Map.entry("000660", "SK하이닉스"),
          Map.entry("373220", "LG에너지솔루션"),
          Map.entry("207940", "삼성바이오로직스"),
          Map.entry("005380", "현대차"),
          Map.entry("000270", "기아"),
          Map.entry("068270", "셀트리온"),
          Map.entry("105560", "KB금융"),
          Map.entry("055550", "신한지주"),
          Map.entry("035420", "NAVER"),
          Map.entry("035720", "카카오"),
          Map.entry("005490", "포스코홀딩스"),
          Map.entry("012330", "현대모비스"),
          Map.entry("051910", "LG화학"),
          Map.entry("006400", "삼성SDI"),
          Map.entry("096770", "SK이노베이션"),
          Map.entry("028260", "삼성물산"),
          Map.entry("086790", "하나금융지주"),
          Map.entry("032830", "삼성생명"),
          Map.entry("066570", "LG전자"),
          Map.entry("017670", "SK텔레콤"),
          Map.entry("030200", "KT"),
          Map.entry("034730", "SK"),
          Map.entry("034020", "두산에너빌리티"),
          Map.entry("012450", "한화에어로스페이스"),
          Map.entry("259960", "크래프톤"),
          Map.entry("329180", "HD현대중공업"),
          Map.entry("036570", "엔씨소프트"),
          Map.entry("251270", "넷마블"),
          Map.entry("323410", "카카오뱅크"),
          Map.entry("326030", "SK바이오팜"),
          Map.entry("051900", "LG생활건강"),
          Map.entry("090430", "아모레퍼시픽"),
          Map.entry("028300", "HLB"),
          Map.entry("247540", "에코프로비엠"),
          Map.entry("086520", "에코프로"),
          Map.entry("000810", "삼성화재"),
          Map.entry("316140", "우리금융지주"),
          Map.entry("015760", "한국전력"),
          Map.entry("009150", "삼성전기"));

  /** 모니터링 대상 섹터 (KRX 업종 기준) */
  private static final List<String> MONITORED_SECTORS =
      List.of(
          "전기·전자", "운송장비·부품", "제약", "은행", "증권", "보험", "IT 서비스", "전기·가스", "화학", "기계·장비", "금속", "통신",
          "유통");

  /**
   * 뉴스 커버리지 현황 조회.
   *
   * @return 커버리지 현황 응답
   */
  public NewsCoverageResponse getCoverageReport() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime since24h = now.minusHours(24);
    LocalDateTime since7d = now.minusDays(7);

    // 24시간/7일 내 뉴스가 있는 종목/섹터 조회
    Set<String> stocksWithNews24h =
        new HashSet<>(processedNewsRepository.findDistinctStockCodesWithNews(since24h));
    Set<String> stocksWithNews7d =
        new HashSet<>(processedNewsRepository.findDistinctStockCodesWithNews(since7d));
    Set<String> sectorsWithNews24h =
        new HashSet<>(processedNewsRepository.findDistinctSectorNamesWithNews(since24h));
    Set<String> sectorsWithNews7d =
        new HashSet<>(processedNewsRepository.findDistinctSectorNamesWithNews(since7d));

    // 뉴스 0건 종목/섹터 찾기
    List<String> zeroCoverageStocks = new ArrayList<>();
    List<StockCoverage> stockCoverages = new ArrayList<>();

    for (Map.Entry<String, String> entry : MONITORED_STOCKS.entrySet()) {
      String code = entry.getKey();
      String name = entry.getValue();

      long count24h = processedNewsRepository.countByStockCodeSince(code, since24h);
      long count7d = processedNewsRepository.countByStockCodeSince(code, since7d);

      if (count7d == 0) {
        zeroCoverageStocks.add(name + " (" + code + ")");
      }

      stockCoverages.add(
          StockCoverage.builder()
              .stockCode(code)
              .stockName(name)
              .newsCount24h(count24h)
              .newsCount7d(count7d)
              .build());
    }

    // 뉴스 0건 섹터 찾기
    List<String> zeroCoverageSectors = new ArrayList<>();
    List<SectorCoverage> sectorCoverages = new ArrayList<>();

    for (String sector : MONITORED_SECTORS) {
      long count24h = processedNewsRepository.countBySectorNameSince(sector, since24h);
      long count7d = processedNewsRepository.countBySectorNameSince(sector, since7d);

      if (count7d == 0) {
        zeroCoverageSectors.add(sector);
      }

      sectorCoverages.add(
          SectorCoverage.builder()
              .sectorName(sector)
              .newsCount24h(count24h)
              .newsCount7d(count7d)
              .build());
    }

    // 종목별 커버리지를 7일 뉴스 개수 내림차순 정렬 후 상위 20개
    stockCoverages.sort((a, b) -> Long.compare(b.getNewsCount7d(), a.getNewsCount7d()));
    List<StockCoverage> topStockCoverages =
        stockCoverages.size() > 20 ? stockCoverages.subList(0, 20) : stockCoverages;

    log.info(
        "Coverage report: stocks with news 24h={}/{}, 7d={}/{}, zero coverage stocks={}",
        stocksWithNews24h.size(),
        MONITORED_STOCKS.size(),
        stocksWithNews7d.size(),
        MONITORED_STOCKS.size(),
        zeroCoverageStocks.size());

    return NewsCoverageResponse.builder()
        .asOf(now)
        .totalStocks(MONITORED_STOCKS.size())
        .stocksWithNews24h(stocksWithNews24h.size())
        .stocksWithNews7d(stocksWithNews7d.size())
        .totalSectors(MONITORED_SECTORS.size())
        .sectorsWithNews24h(sectorsWithNews24h.size())
        .sectorsWithNews7d(sectorsWithNews7d.size())
        .zeroCoverageStocks(zeroCoverageStocks)
        .zeroCoverageSectors(zeroCoverageSectors)
        .stockCoverages(topStockCoverages)
        .sectorCoverages(sectorCoverages)
        .build();
  }
}
