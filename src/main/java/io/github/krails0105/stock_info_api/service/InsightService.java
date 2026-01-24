package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockFinancialResponse.KrxStockFinancialItem;
import io.github.krails0105.stock_info_api.dto.insight.InsightMeta;
import io.github.krails0105.stock_info_api.dto.insight.InsightMeta.Source;
import io.github.krails0105.stock_info_api.dto.insight.InsightNews;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import io.github.krails0105.stock_info_api.dto.insight.SectorInsight;
import io.github.krails0105.stock_info_api.dto.insight.SectorInsight.SectorEntity;
import io.github.krails0105.stock_info_api.dto.insight.SectorInsight.SectorSummary;
import io.github.krails0105.stock_info_api.dto.insight.SectorInsight.TopPick;
import io.github.krails0105.stock_info_api.dto.insight.SectorInsight.TopPick.PickType;
import io.github.krails0105.stock_info_api.dto.insight.SectorInsight.TopPick.TopPickRole;
import io.github.krails0105.stock_info_api.dto.insight.StockInsight;
import io.github.krails0105.stock_info_api.dto.response.StockListItem;
import io.github.krails0105.stock_info_api.service.rule.RuleConstants;
import io.github.krails0105.stock_info_api.service.rule.RuleEngineService;
import io.github.krails0105.stock_info_api.service.rule.StockSignals;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 인사이트 생성 통합 서비스. 기존 StockService/SectorService에서 데이터를 가져와 RuleEngineService로 인사이트를 생성. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

  /** P0-1: 표본 부족 경고 임계치 (5개 미만이면 경고) */
  private static final int LOW_SAMPLE_THRESHOLD = 5;

  /** P0-3: 섹터 대표 종목 역할 결정 임계 점수 (섹터 평균 점수 < 60이면 REPRESENTATIVE) */
  private static final int REPRESENTATIVE_SCORE_THRESHOLD = 60;

  private final StockService stockService;
  private final SectorService sectorService;
  private final RuleEngineService ruleEngineService;
  private final NewsAggregatorService newsAggregatorService;

  /**
   * 종목 인사이트 생성
   *
   * @param stockCode 종목 코드
   * @return StockInsight
   */
  public StockInsight getStockInsight(String stockCode) {
    // 기존 서비스에서 데이터 조회
    KrxStockFinancialItem financialItem = stockService.getStockById(stockCode);

    if (financialItem == null) {
      throw new IllegalArgumentException("Stock not found: " + stockCode);
    }

    // 섹터 통계 계산을 위해 같은 섹터 종목 조회
    // 현재 KrxStockFinancialItem에는 sectorName이 없으므로 기본값 사용
    String sectorName = "전체"; // TODO: 섹터 정보 연동 필요
    Map<String, Double> sectorMedians = calculateSectorMedians(sectorName);

    // 뉴스 조회
    List<NewsItem> newsItems = newsAggregatorService.getNewsByStockCode(stockCode);

    // StockSignals 생성
    StockSignals signals = buildStockSignals(financialItem, sectorMedians, newsItems);

    // RuleEngine으로 인사이트 생성
    return ruleEngineService.buildStockInsight(signals);
  }

  /**
   * 섹터 인사이트 생성
   *
   * @param sectorName 섹터명
   * @return SectorInsight
   */
  public SectorInsight getSectorInsight(String sectorName) {
    // 섹터 정보 조회
    List<SectorScoreDto> allSectors = sectorService.getAllSectors();
    SectorScoreDto sector =
        allSectors.stream()
            .filter(s -> s.getSectorName().equals(sectorName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Sector not found: " + sectorName));

    // 섹터별 종목 조회
    List<StockListItem> stocks = sectorService.getStocksBySectorName(sectorName);

    // P0-1: 표본 크기 정보 계산
    int sampleSize = stocks != null ? stocks.size() : 0;
    boolean lowSampleWarning = sampleSize < LOW_SAMPLE_THRESHOLD;

    // 섹터 통계 계산
    Map<String, Double> sectorMedians = calculateSectorMediansFromStocks(stocks);

    // P0-3: TopPick 역할 결정
    TopPickRole role = determineTopPickRole(sector, stocks);

    // Top Picks 생성 (역할 포함)
    List<TopPick> topPicks = buildTopPicks(stocks, sectorMedians, role);

    // P0-3: 섹션 타이틀 생성
    String sectionTitle = buildSectionTitle(role, topPicks.size());

    // 섹터 브리핑 생성
    SectorSummary summary = buildSectorSummary(sector, stocks);

    // 뉴스 조회 및 InsightNews 생성
    InsightNews news = buildSectorNews(sectorName);

    return SectorInsight.builder()
        .entity(SectorEntity.builder().name(sectorName).build())
        .meta(
            InsightMeta.builder()
                .asOf(LocalDateTime.now())
                .sources(List.of(Source.KRX))
                .coverage(0.8)
                .stalenessSec(0)
                .build())
        .summary(summary)
        .topPicks(topPicks)
        .sectionTitle(sectionTitle)
        .news(news)
        .sampleSize(sampleSize)
        .lowSampleWarning(lowSampleWarning)
        .build();
  }

  // ==================== Private Methods ====================

  private StockSignals buildStockSignals(
      KrxStockFinancialItem item, Map<String, Double> sectorMedians, List<NewsItem> newsItems) {

    double coverage = calculateCoverage(item);

    // Convert primitive double to Double (wrapper) - use null for zero/missing values
    Double per = item.getPer() > 0 ? item.getPer() : null;
    Double pbr = item.getPbr() > 0 ? item.getPbr() : null;
    Double forwardPer = item.getForwardPer() > 0 ? item.getForwardPer() : null;
    Double eps = item.getEps() != 0 ? item.getEps() : null;
    Double bps = item.getBps() != 0 ? item.getBps() : null;
    Double dividendYield = item.getDividendYield() > 0 ? item.getDividendYield() : null;

    return StockSignals.builder()
        .stockCode(item.getStockCode())
        .stockName(item.getStockName())
        .sectorName("전체") // TODO: 섹터 정보 연동
        .market("KOSPI") // TODO: 시장 정보 연동
        .price(item.getClosingPrice())
        .priceChange(item.getPriceChange())
        .changeRate(item.getChangeRate())
        .per(per)
        .pbr(pbr)
        .forwardPer(forwardPer)
        .eps(eps)
        .bps(bps)
        .dividendYield(dividendYield)
        .sectorMedianPer(sectorMedians.get("per"))
        .sectorMedianPbr(sectorMedians.get("pbr"))
        .sectorMedianRoe(sectorMedians.get("roe"))
        .sectorMedianVolatility(sectorMedians.get("volatility"))
        .dataCoverage(coverage)
        .isSuspended(false)
        .isAdministrative(false)
        .hasDeficit(item.getPer() <= 0)
        .newsItems(newsItems != null ? newsItems : List.of())
        .build();
  }

  private double calculateCoverage(KrxStockFinancialItem item) {
    int totalFields = 8;
    int presentFields = 0;

    // primitive types use 0 as "missing" indicator
    if (item.getPer() > 0) presentFields++;
    if (item.getPbr() > 0) presentFields++;
    if (item.getEps() != 0) presentFields++;
    if (item.getBps() != 0) presentFields++;
    if (item.getForwardPer() > 0) presentFields++;
    if (item.getForwardEps() != 0) presentFields++;
    if (item.getDividendYield() > 0) presentFields++;
    if (item.getClosingPrice() > 0) presentFields++;

    return (double) presentFields / totalFields;
  }

  private Map<String, Double> calculateSectorMedians(String sectorName) {
    // 기본값 반환 (실제로는 섹터별 종목에서 계산)
    return Map.of(
        "per", 15.0,
        "pbr", 1.5,
        "roe", 10.0,
        "volatility", 2.0);
  }

  private Map<String, Double> calculateSectorMediansFromStocks(List<StockListItem> stocks) {
    if (stocks == null || stocks.isEmpty()) {
      return calculateSectorMedians("전체");
    }

    DoubleSummaryStatistics perStats =
        stocks.stream()
            .filter(s -> s.getPer() != null && s.getPer() > 0)
            .mapToDouble(StockListItem::getPer)
            .summaryStatistics();

    DoubleSummaryStatistics pbrStats =
        stocks.stream()
            .filter(s -> s.getPbr() != null && s.getPbr() > 0)
            .mapToDouble(StockListItem::getPbr)
            .summaryStatistics();

    return Map.of(
        "per",
        perStats.getCount() > 0 ? perStats.getAverage() : 15.0,
        "pbr",
        pbrStats.getCount() > 0 ? pbrStats.getAverage() : 1.5,
        "roe",
        10.0,
        "volatility",
        2.0);
  }

  private List<TopPick> buildTopPicks(
      List<StockListItem> stocks, Map<String, Double> sectorMedians, TopPickRole role) {
    if (stocks == null || stocks.isEmpty()) {
      return List.of();
    }

    // 점수 상위 5개 선택
    List<StockListItem> topStocks =
        stocks.stream()
            .sorted(Comparator.comparingInt(StockListItem::getScore).reversed())
            .limit(RuleConstants.DEFAULT_TOP_PICKS_COUNT)
            .collect(Collectors.toList());

    List<TopPick> topPicks = new ArrayList<>();
    for (StockListItem stock : topStocks) {
      PickType pickType = determinePickType(stock, sectorMedians);
      List<String> reasons = generatePickReasons(stock, pickType, sectorMedians);
      String caution = generatePickCaution(stock, sectorMedians);

      topPicks.add(
          TopPick.builder()
              .code(stock.getCode())
              .name(stock.getName())
              .grade(stock.getLabel())
              .pickType(pickType)
              .reasons(reasons)
              .caution(caution)
              .role(role)
              .build());
    }

    return topPicks;
  }

  /**
   * P0-3: TopPick 역할 결정
   *
   * <p>섹터가 STRONG이고 종목 평균 점수가 60 미만이면 REPRESENTATIVE (섹터 대표 종목), 그 외에는 WATCHLIST_PRIORITY (우선 관찰
   * 종목)
   */
  private TopPickRole determineTopPickRole(SectorScoreDto sector, List<StockListItem> stocks) {
    if (sector.getLabel() != ScoreLabel.STRONG) {
      return TopPickRole.WATCHLIST_PRIORITY;
    }

    // 상위 종목들의 평균 점수 계산
    double avgScore =
        stocks.stream()
            .sorted(Comparator.comparingInt(StockListItem::getScore).reversed())
            .limit(RuleConstants.DEFAULT_TOP_PICKS_COUNT)
            .mapToInt(StockListItem::getScore)
            .average()
            .orElse(0.0);

    if (avgScore < REPRESENTATIVE_SCORE_THRESHOLD) {
      return TopPickRole.REPRESENTATIVE;
    }

    return TopPickRole.WATCHLIST_PRIORITY;
  }

  /**
   * P0-3: 섹션 타이틀 생성
   *
   * <p>REPRESENTATIVE: "섹터 대표 종목 Top N", WATCHLIST_PRIORITY: "우선 검토 종목 Top N"
   */
  private String buildSectionTitle(TopPickRole role, int count) {
    return switch (role) {
      case REPRESENTATIVE -> String.format("섹터 대표 종목 Top %d", count);
      case WATCHLIST_PRIORITY -> String.format("우선 검토 종목 Top %d", count);
    };
  }

  private PickType determinePickType(StockListItem stock, Map<String, Double> sectorMedians) {
    Double per = stock.getPer();
    Double pbr = stock.getPbr();
    Double medianPer = sectorMedians.get("per");
    Double medianPbr = sectorMedians.get("pbr");

    // 밸류형: PER/PBR이 섹터 중앙값 대비 낮음
    if (per != null && medianPer != null && per > 0 && per < medianPer * 0.8) {
      return PickType.VALUE;
    }
    if (pbr != null && medianPbr != null && pbr > 0 && pbr < medianPbr * 0.8) {
      return PickType.VALUE;
    }

    // 안정형: 점수 STRONG + 큰 변동 없음
    if (stock.getLabel() == ScoreLabel.STRONG) {
      return PickType.STABLE;
    }

    // 모멘텀형: 등락률 양수
    double changeRate = parseChangeRate(stock.getChangeRate());
    if (changeRate > 0) {
      return PickType.MOMENTUM;
    }

    // 기본
    return PickType.WATCH;
  }

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

  private List<String> generatePickReasons(
      StockListItem stock, PickType pickType, Map<String, Double> sectorMedians) {
    List<String> reasons = new ArrayList<>();

    switch (pickType) {
      case VALUE -> {
        reasons.add("섹터 대비 밸류에이션 부담이 낮은 편이에요.");
        if (stock.getPer() != null && stock.getPer() > 0) {
          reasons.add(String.format("PER %.1f배로 섹터 평균 대비 저평가 구간이에요.", stock.getPer()));
        }
      }
      case STABLE -> {
        reasons.add("초보자가 살펴보기 좋은 안정적인 종목이에요.");
        reasons.add("섹터 내에서 상대적으로 안정적인 흐름을 보여요.");
      }
      case MOMENTUM -> {
        reasons.add("최근 상승 흐름이 있어 관심을 가져볼 만해요.");
        if (stock.getChangeRate() != null) {
          reasons.add(String.format("등락률 %s로 상승세에요.", stock.getChangeRate()));
        }
      }
      case GROWTH -> {
        reasons.add("성장 가능성이 기대되는 종목이에요.");
        reasons.add("실적 개선 추세가 확인되면 강해질 수 있어요.");
      }
      case WATCH -> {
        reasons.add("관찰 리스트에 올려두고 지켜볼 만한 종목이에요.");
        reasons.add("추가적인 정보 확인 후 판단이 필요해요.");
      }
    }

    // 최소 2개, 최대 3개
    if (reasons.size() < 2) {
      reasons.add("더 많은 정보를 확인해 보세요.");
    }

    return reasons.stream().limit(3).collect(Collectors.toList());
  }

  private String generatePickCaution(StockListItem stock, Map<String, Double> sectorMedians) {
    // 고PER 경고
    if (stock.getPer() != null && sectorMedians.get("per") != null) {
      if (stock.getPer() > sectorMedians.get("per") * 1.3) {
        return "기대가 반영된 가격이라 실적 확인이 중요해요.";
      }
    }

    // 적자 경고
    if (stock.getPer() != null && stock.getPer() <= 0) {
      return "적자 가능성이 있어 재무제표 확인이 필요해요.";
    }

    // 하락 경고
    double changeRate = parseChangeRate(stock.getChangeRate());
    if (changeRate < -3) {
      return "최근 하락세라 추가 하락 가능성을 고려하세요.";
    }

    return null;
  }

  private SectorSummary buildSectorSummary(SectorScoreDto sector, List<StockListItem> stocks) {
    String headline = generateSectorHeadline(sector);
    List<String> drivers = generateSectorDrivers(sector, stocks);

    return SectorSummary.builder().headline(headline).drivers(drivers).build();
  }

  private String generateSectorHeadline(SectorScoreDto sector) {
    ScoreLabel label = sector.getLabel();
    String sectorName = sector.getSectorName();

    return switch (label) {
      case STRONG -> String.format("%s 섹터는 현재 강세 흐름을 보이고 있어요.", sectorName);
      case NEUTRAL -> String.format("%s 섹터는 현재 보합세를 유지하고 있어요.", sectorName);
      case WEAK -> String.format("%s 섹터는 현재 약세 흐름이라 신중한 접근이 필요해요.", sectorName);
    };
  }

  private List<String> generateSectorDrivers(SectorScoreDto sector, List<StockListItem> stocks) {
    List<String> drivers = new ArrayList<>();

    // 상승 종목 비율 (0-100 정수)
    int risingRatio = sector.getRisingStockRatio();
    if (risingRatio > 60) {
      drivers.add("섹터 내 상승 종목이 많아 전반적으로 긍정적이에요.");
    } else if (risingRatio < 40) {
      drivers.add("섹터 내 하락 종목이 많아 전반적으로 조심스러워요.");
    }

    // 종목 수
    int stockCount = sector.getStockCount();
    if (stockCount > 0) {
      drivers.add(String.format("총 %d개 종목으로 구성되어 있어요.", stockCount));
    }

    // 기본 드라이버
    if (drivers.isEmpty()) {
      drivers.add("섹터 전반의 흐름을 참고하세요.");
    }

    return drivers.stream().limit(3).collect(Collectors.toList());
  }

  /**
   * 섹터 뉴스 조회 및 InsightNews 생성.
   *
   * @param sectorName 섹터명
   * @return InsightNews
   */
  private InsightNews buildSectorNews(String sectorName) {
    List<NewsItem> newsItems = newsAggregatorService.getNewsBySectorName(sectorName);

    if (newsItems == null || newsItems.isEmpty()) {
      return InsightNews.builder().issueBrief(List.of()).headlineItems(List.of()).build();
    }

    // 이슈 브리프: HIGH 중요도 뉴스 제목 최대 2개
    List<String> issueBrief =
        newsItems.stream()
            .filter(n -> n.getImportance() == NewsItem.Importance.HIGH)
            .limit(2)
            .map(NewsItem::getTitle)
            .collect(Collectors.toList());

    // 헤드라인 아이템: 최대 5개
    List<NewsItem> headlineItems = newsItems.stream().limit(5).collect(Collectors.toList());

    return InsightNews.builder().issueBrief(issueBrief).headlineItems(headlineItems).build();
  }
}
