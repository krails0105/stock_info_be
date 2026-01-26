package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import io.github.krails0105.stock_info_api.dto.response.HomePicksResponse;
import io.github.krails0105.stock_info_api.dto.response.StockListItem;
import io.github.krails0105.stock_info_api.dto.response.StockPickCard;
import io.github.krails0105.stock_info_api.dto.response.StockPickCard.PickBucket;
import io.github.krails0105.stock_info_api.dto.response.StockPickCard.PickNews;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 홈 Watchlist Picks 서비스.
 *
 * <p>메인 페이지에서 "오늘 주목할 종목 5~10개"를 선정하는 로직을 담당. 버킷별로 종목을 분류하고 다양성 제약을 적용하여 최종 리스트 생성.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomePicksService {

  /** 동일 섹터 최대 선정 수 */
  private static final int MAX_SAME_SECTOR = 2;

  /** 동일 버킷 최대 선정 수 */
  private static final int MAX_SAME_BUCKET = 3;

  /** 섹터 내 최소 종목 수 (표본 보정) */
  private static final int MIN_COVERAGE_THRESHOLD = 5;

  /** 상위 섹터 조회 수 */
  private static final int TOP_SECTOR_COUNT = 5;

  private final SectorService sectorService;
  private final NewsAggregatorService newsAggregatorService;

  /**
   * 홈 Watchlist Picks 조회
   *
   * @param size 선정할 종목 수 (기본 8)
   * @param preset 프리셋 (default/stable/momentum/value)
   * @return HomePicksResponse
   */
  public HomePicksResponse getHomePicks(int size, String preset) {
    List<StockPickCard> allCandidates = new ArrayList<>();

    // 1. 상위 섹터 조회 (표본 보정 적용)
    List<SectorScoreDto> topSectors = getTopSectors(TOP_SECTOR_COUNT);
    log.debug("Top sectors for home picks: {}", topSectors.size());

    // 2. 각 섹터에서 후보 종목 수집
    for (SectorScoreDto sector : topSectors) {
      List<StockListItem> stocks = sectorService.getStocksBySectorName(sector.getSectorName());
      if (stocks == null || stocks.isEmpty()) {
        continue;
      }

      // 섹터별 종목 평균 PER
      double avgPer = calculateAvgPer(stocks);

      // 상위 10개 종목만 후보로
      List<StockListItem> topStocks =
          stocks.stream()
              .sorted(Comparator.comparingInt(StockListItem::getScore).reversed())
              .limit(10)
              .collect(Collectors.toList());

      for (StockListItem stock : topStocks) {
        PickBucket bucket = determineBucket(stock, sector, avgPer);
        List<String> reasons = generateReasons(stock, sector, bucket);
        String caution = generateCaution(stock);
        PickNews news = getStockNews(stock.getCode());

        allCandidates.add(
            StockPickCard.builder()
                .code(stock.getCode())
                .name(stock.getName())
                .sectorName(sector.getSectorName())
                .scoreValue(stock.getScore())
                .grade(stock.getLabel())
                .pickBucket(bucket)
                .reasons(reasons)
                .caution(caution)
                .news(news)
                .build());
      }
    }

    log.debug("Total candidates collected: {}", allCandidates.size());

    // 3. 다양성 제약 적용 후 최종 선정
    List<StockPickCard> finalPicks = selectWithDiversity(allCandidates, size, preset);
    log.info("Home picks selected: {} items (preset: {})", finalPicks.size(), preset);

    return HomePicksResponse.builder()
        .asOf(LocalDateTime.now())
        .preset(preset)
        .items(finalPicks)
        .build();
  }

  /** 상위 섹터 조회 (표본 보정 적용) */
  private List<SectorScoreDto> getTopSectors(int limit) {
    return sectorService.getAllSectors().stream()
        .filter(s -> s.getStockCount() >= MIN_COVERAGE_THRESHOLD)
        .sorted(Comparator.comparingInt(SectorScoreDto::getScore).reversed())
        .limit(limit)
        .collect(Collectors.toList());
  }

  /** 섹터 평균 PER 계산 */
  private double calculateAvgPer(List<StockListItem> stocks) {
    return stocks.stream()
        .filter(s -> s.getPer() != null && s.getPer() > 0)
        .mapToDouble(StockListItem::getPer)
        .average()
        .orElse(15.0);
  }

  /** 종목 버킷 결정 */
  private PickBucket determineBucket(StockListItem stock, SectorScoreDto sector, double avgPer) {
    // 저평가: PER이 섹터 평균의 70% 이하
    if (stock.getPer() != null && stock.getPer() > 0 && stock.getPer() < avgPer * 0.7) {
      return PickBucket.VALUE;
    }

    // 섹터 대표: 강세 섹터의 고점수 종목
    if (sector.getLabel() == ScoreLabel.STRONG && stock.getScore() >= 70) {
      return PickBucket.REPRESENTATIVE;
    }

    // 모멘텀: 양의 등락률 1% 이상
    double changeRate = parseChangeRate(stock.getChangeRate());
    if (changeRate > 1.0) {
      return PickBucket.MOMENTUM;
    }

    // 기본: 안정형 (초보자 추천)
    return PickBucket.STABLE;
  }

  /** 종목 선정 이유 생성 (2개) */
  private List<String> generateReasons(
      StockListItem stock, SectorScoreDto sector, PickBucket bucket) {
    List<String> reasons = new ArrayList<>();

    // 첫 번째 이유: 섹터 연결
    if (sector.getLabel() == ScoreLabel.STRONG) {
      reasons.add(String.format("%s 섹터 상위권 흐름", sector.getSectorName()));
    } else {
      reasons.add(String.format("%s 섹터 내 주목 종목", sector.getSectorName()));
    }

    // 두 번째 이유: 버킷별 강점
    switch (bucket) {
      case VALUE -> reasons.add("섹터 대비 밸류에이션 매력");
      case REPRESENTATIVE -> reasons.add("섹터 대표주로서 안정적");
      case MOMENTUM -> reasons.add(String.format("최근 %s 상승 흐름", stock.getChangeRate()));
      case STABLE -> reasons.add("초보자가 검토하기 좋은 종목");
      default -> reasons.add("관찰 리스트 후보");
    }

    return reasons.stream().limit(2).collect(Collectors.toList());
  }

  /** 주의사항 생성 */
  private String generateCaution(StockListItem stock) {
    // 적자 경고
    if (stock.getPer() != null && stock.getPer() <= 0) {
      return "적자 가능성, 재무 확인 필요";
    }

    // 급등락 경고
    double changeRate = parseChangeRate(stock.getChangeRate());
    if (Math.abs(changeRate) > 5) {
      return "변동성 큼, 신중한 접근 권장";
    }

    return null;
  }

  /** 종목 뉴스 조회 */
  private PickNews getStockNews(String stockCode) {
    try {
      List<NewsItem> newsItems = newsAggregatorService.getNewsByStockCode(stockCode);
      if (newsItems == null || newsItems.isEmpty()) {
        return null;
      }

      NewsItem topNews = newsItems.get(0);
      return PickNews.builder()
          .title(topNews.getTitle())
          .url(topNews.getUrl())
          .publisher(topNews.getPublisher())
          .publishedAt(
              topNews.getPublishedAt() != null ? topNews.getPublishedAt().toString() : null)
          .build();
    } catch (Exception e) {
      log.debug("Failed to get news for stock {}: {}", stockCode, e.getMessage());
      return null;
    }
  }

  /** 다양성 제약 적용하여 최종 선정 */
  private List<StockPickCard> selectWithDiversity(
      List<StockPickCard> candidates, int size, String preset) {
    // 프리셋별 필터
    List<StockPickCard> filtered = filterByPreset(candidates, preset);

    // 점수 내림차순 정렬
    filtered.sort(Comparator.comparingInt(StockPickCard::getScoreValue).reversed());

    // 다양성 제약 적용
    List<StockPickCard> result = new ArrayList<>();
    Map<String, Integer> sectorCount = new HashMap<>();
    Map<PickBucket, Integer> bucketCount = new HashMap<>();

    for (StockPickCard card : filtered) {
      if (result.size() >= size) {
        break;
      }

      int sCount = sectorCount.getOrDefault(card.getSectorName(), 0);
      int bCount = bucketCount.getOrDefault(card.getPickBucket(), 0);

      if (sCount < MAX_SAME_SECTOR && bCount < MAX_SAME_BUCKET) {
        result.add(card);
        sectorCount.put(card.getSectorName(), sCount + 1);
        bucketCount.put(card.getPickBucket(), bCount + 1);
      }
    }

    return result;
  }

  /** 프리셋별 필터링 */
  private List<StockPickCard> filterByPreset(List<StockPickCard> candidates, String preset) {
    if (preset == null || preset.equals("default")) {
      return new ArrayList<>(candidates);
    }

    return switch (preset) {
      case "stable" ->
          candidates.stream()
              .filter(c -> c.getPickBucket() == PickBucket.STABLE)
              .collect(Collectors.toList());
      case "momentum" ->
          candidates.stream()
              .filter(c -> c.getPickBucket() == PickBucket.MOMENTUM)
              .collect(Collectors.toList());
      case "value" ->
          candidates.stream()
              .filter(c -> c.getPickBucket() == PickBucket.VALUE)
              .collect(Collectors.toList());
      default -> new ArrayList<>(candidates);
    };
  }

  /** 등락률 문자열 파싱 */
  private double parseChangeRate(String changeRate) {
    if (changeRate == null || changeRate.isEmpty()) {
      return 0.0;
    }
    try {
      return Double.parseDouble(changeRate.replace("%", "").replace("+", ""));
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }
}
