package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockFinancialResponse.KrxStockFinancialItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class MockStockDataProvider implements StockDataProvider {

  private final Map<String, StockScoreDto> stockMap;
  private final Map<String, List<StockScoreDto>> stocksBySector;

  public MockStockDataProvider() {
    List<StockScoreDto> allStocks = initMockData();
    this.stockMap =
        allStocks.stream().collect(Collectors.toMap(StockScoreDto::getCode, Function.identity()));
    this.stocksBySector =
        allStocks.stream()
            .collect(Collectors.groupingBy(StockScoreDto::getSectorId, Collectors.toList()));
  }

  private List<StockScoreDto> initMockData() {
    List<StockScoreDto> stocks = new ArrayList<>();

    // 기술주 섹터
    stocks.add(
        StockScoreDto.builder()
            .code("005930")
            .name("삼성전자")
            .score(85)
            .label(ScoreLabel.STRONG)
            .price(72500)
            .priceChange("+2.5%")
            .returnGrade("높음")
            .valuationGrade("저평가")
            .volumeGrade("증가")
            .sectorId("tech")
            .sectorName("기술주")
            .sectorScore(82)
            .reasons(List.of("최근 수익률 상위 20%", "PER 업종 평균 대비 저평가", "거래량 25% 증가"))
            .build());

    stocks.add(
        StockScoreDto.builder()
            .code("000660")
            .name("SK하이닉스")
            .score(78)
            .label(ScoreLabel.STRONG)
            .price(135000)
            .priceChange("+3.8%")
            .returnGrade("높음")
            .valuationGrade("적정")
            .volumeGrade("급증")
            .sectorId("tech")
            .sectorName("기술주")
            .sectorScore(82)
            .reasons(List.of("1주 수익률 +3.8%", "밸류에이션 적정 수준", "거래량 급증"))
            .build());

    stocks.add(
        StockScoreDto.builder()
            .code("035420")
            .name("NAVER")
            .score(72)
            .label(ScoreLabel.STRONG)
            .price(215000)
            .priceChange("+1.2%")
            .returnGrade("보통")
            .valuationGrade("저평가")
            .volumeGrade("증가")
            .sectorId("tech")
            .sectorName("기술주")
            .sectorScore(82)
            .reasons(List.of("PER 업종 평균 대비 저평가", "거래량 소폭 증가", "섹터 강세 영향"))
            .build());

    stocks.add(
        StockScoreDto.builder()
            .code("035720")
            .name("카카오")
            .score(65)
            .label(ScoreLabel.NEUTRAL)
            .price(52000)
            .priceChange("-0.5%")
            .returnGrade("보통")
            .valuationGrade("적정")
            .volumeGrade("보통")
            .sectorId("tech")
            .sectorName("기술주")
            .sectorScore(82)
            .reasons(List.of("최근 수익률 보통", "밸류에이션 적정", "거래량 변동 없음"))
            .build());

    // 바이오 섹터
    stocks.add(
        StockScoreDto.builder()
            .code("207940")
            .name("삼성바이오로직스")
            .score(80)
            .label(ScoreLabel.STRONG)
            .price(815000)
            .priceChange("+4.2%")
            .returnGrade("높음")
            .valuationGrade("적정")
            .volumeGrade("급증")
            .sectorId("bio")
            .sectorName("바이오")
            .sectorScore(75)
            .reasons(List.of("1주 수익률 +4.2%", "거래량 50% 급증", "신약 기대감"))
            .build());

    stocks.add(
        StockScoreDto.builder()
            .code("068270")
            .name("셀트리온")
            .score(73)
            .label(ScoreLabel.STRONG)
            .price(178000)
            .priceChange("+2.1%")
            .returnGrade("보통")
            .valuationGrade("저평가")
            .volumeGrade("증가")
            .sectorId("bio")
            .sectorName("바이오")
            .sectorScore(75)
            .reasons(List.of("PBR 업종 대비 저평가", "거래량 20% 증가", "실적 개선 기대"))
            .build());

    // 금융 섹터
    stocks.add(
        StockScoreDto.builder()
            .code("105560")
            .name("KB금융")
            .score(74)
            .label(ScoreLabel.STRONG)
            .price(68500)
            .priceChange("+1.8%")
            .returnGrade("보통")
            .valuationGrade("저평가")
            .volumeGrade("증가")
            .sectorId("finance")
            .sectorName("금융")
            .sectorScore(71)
            .reasons(List.of("배당 수익률 높음", "PER 저평가", "금리 상승 수혜"))
            .build());

    stocks.add(
        StockScoreDto.builder()
            .code("055550")
            .name("신한지주")
            .score(70)
            .label(ScoreLabel.STRONG)
            .price(45200)
            .priceChange("+1.5%")
            .returnGrade("보통")
            .valuationGrade("저평가")
            .volumeGrade("보통")
            .sectorId("finance")
            .sectorName("금융")
            .sectorScore(71)
            .reasons(List.of("안정적 배당", "밸류에이션 매력", "실적 안정"))
            .build());

    // 에너지 섹터
    stocks.add(
        StockScoreDto.builder()
            .code("096770")
            .name("SK이노베이션")
            .score(52)
            .label(ScoreLabel.NEUTRAL)
            .price(125000)
            .priceChange("-0.8%")
            .returnGrade("낮음")
            .valuationGrade("적정")
            .volumeGrade("감소")
            .sectorId("energy")
            .sectorName("에너지")
            .sectorScore(55)
            .reasons(List.of("유가 변동성 영향", "거래량 감소세", "배터리 사업 기대"))
            .build());

    // 소비재 섹터
    stocks.add(
        StockScoreDto.builder()
            .code("051910")
            .name("LG화학")
            .score(45)
            .label(ScoreLabel.NEUTRAL)
            .price(385000)
            .priceChange("-1.2%")
            .returnGrade("낮음")
            .valuationGrade("고평가")
            .volumeGrade("감소")
            .sectorId("consumer")
            .sectorName("소비재")
            .sectorScore(48)
            .reasons(List.of("최근 수익률 부진", "밸류에이션 부담", "섹터 약세 영향"))
            .build());

    // 자동차 섹터
    stocks.add(
        StockScoreDto.builder()
            .code("005380")
            .name("현대차")
            .score(38)
            .label(ScoreLabel.WEAK)
            .price(185000)
            .priceChange("-2.5%")
            .returnGrade("낮음")
            .valuationGrade("적정")
            .volumeGrade("감소")
            .sectorId("automotive")
            .sectorName("자동차")
            .sectorScore(35)
            .reasons(List.of("1주 수익률 -2.5%", "거래량 15% 감소", "섹터 전반 약세"))
            .build());

    stocks.add(
        StockScoreDto.builder()
            .code("000270")
            .name("기아")
            .score(32)
            .label(ScoreLabel.WEAK)
            .price(92000)
            .priceChange("-3.1%")
            .returnGrade("낮음")
            .valuationGrade("저평가")
            .volumeGrade("감소")
            .sectorId("automotive")
            .sectorName("자동차")
            .sectorScore(35)
            .reasons(List.of("1주 수익률 -3.1%", "섹터 약세 영향", "밸류에이션만 매력"))
            .build());

    return stocks;
  }

  @Override
  public List<StockInfo> getAllStocks() {
    return List.of();
  }

  @Override
  public KrxStockFinancialItem getStocksByStockId(String stockId) {
    StockScoreDto stock = stockMap.get(stockId);
    if (stock == null) {
      return null;
    }
    // Mock 재무 데이터 생성 (점수에 따라 PER/PBR 결정)
    double per = stock.getScore() >= 70 ? 12.5 : (stock.getScore() >= 40 ? 18.0 : 25.0);
    double pbr = stock.getScore() >= 70 ? 1.2 : (stock.getScore() >= 40 ? 1.8 : 2.5);
    return KrxStockFinancialItem.builder()
        .stockCode(stock.getCode())
        .stockName(stock.getName())
        .closingPrice(stock.getPrice())
        .priceChange(0)
        .changeRate(parseChangeRate(stock.getPriceChange()))
        .eps(stock.getPrice() / per)
        .per(per)
        .bps(stock.getPrice() / pbr)
        .pbr(pbr)
        .forwardEps(stock.getPrice() / (per * 0.9))
        .forwardPer(per * 0.9)
        .dividendPerShare((long) (stock.getPrice() * 0.02))
        .dividendYield(2.0)
        .build();
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

  @Override
  public List<StockScoreDto> getStocksBySector(String sectorId) {
    return stocksBySector.getOrDefault(sectorId, List.of());
  }

  @Override
  public StockScoreDto getStockByCode(String code) {
    return stockMap.get(code);
  }

  @Override
  public List<StockScoreDto> searchStocks(String keyword) {
    String lowerKeyword = keyword.toLowerCase();
    return stockMap.values().stream()
        .filter(
            stock ->
                stock.getName().toLowerCase().contains(lowerKeyword)
                    || stock.getCode().contains(keyword))
        .collect(Collectors.toList());
  }

  @Override
  public List<StockScoreDto> getTopStocksBySector(String sectorId, int limit) {
    return stocksBySector.getOrDefault(sectorId, List.of()).stream()
        .sorted(Comparator.comparingInt(StockScoreDto::getScore).reversed())
        .limit(limit)
        .collect(Collectors.toList());
  }
}
