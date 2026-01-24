package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class MockSectorDataProvider implements SectorDataProvider {

  private final Map<String, SectorScoreDto> sectorMap;
  private final List<SectorScoreDto> sectors;
  private final Map<String, List<KrxStockItem>> stocksBySector;

  public MockSectorDataProvider() {
    this.sectors = initMockData();
    this.sectorMap =
        sectors.stream()
            .collect(Collectors.toMap(SectorScoreDto::getSectorId, Function.identity()));
    this.stocksBySector = initMockStocks();
  }

  private List<SectorScoreDto> initMockData() {
    return List.of(
        SectorScoreDto.builder()
            .sectorId("tech")
            .sectorName("기술주")
            .score(82)
            .label(ScoreLabel.STRONG)
            .weekReturn("+4.2%")
            .volumeChange("+28%")
            .risingStockRatio(65)
            .reasons(List.of("1주 수익률 +4.2%", "거래량 28% 증가", "상승 종목 비율 65%"))
            .stockCount(45)
            .build(),
        SectorScoreDto.builder()
            .sectorId("bio")
            .sectorName("바이오")
            .score(75)
            .label(ScoreLabel.STRONG)
            .weekReturn("+3.1%")
            .volumeChange("+45%")
            .risingStockRatio(58)
            .reasons(List.of("1주 수익률 +3.1%", "거래량 45% 급증", "상승 종목 비율 58%"))
            .stockCount(38)
            .build(),
        SectorScoreDto.builder()
            .sectorId("finance")
            .sectorName("금융")
            .score(71)
            .label(ScoreLabel.STRONG)
            .weekReturn("+2.8%")
            .volumeChange("+15%")
            .risingStockRatio(62)
            .reasons(List.of("1주 수익률 +2.8%", "거래량 15% 증가", "상승 종목 비율 62%"))
            .stockCount(32)
            .build(),
        SectorScoreDto.builder()
            .sectorId("energy")
            .sectorName("에너지")
            .score(55)
            .label(ScoreLabel.NEUTRAL)
            .weekReturn("+0.8%")
            .volumeChange("-5%")
            .risingStockRatio(48)
            .reasons(List.of("1주 수익률 +0.8%", "거래량 5% 감소", "상승 종목 비율 48%"))
            .stockCount(25)
            .build(),
        SectorScoreDto.builder()
            .sectorId("consumer")
            .sectorName("소비재")
            .score(48)
            .label(ScoreLabel.NEUTRAL)
            .weekReturn("-0.5%")
            .volumeChange("+3%")
            .risingStockRatio(42)
            .reasons(List.of("1주 수익률 -0.5%", "거래량 3% 증가", "상승 종목 비율 42%"))
            .stockCount(52)
            .build(),
        SectorScoreDto.builder()
            .sectorId("automotive")
            .sectorName("자동차")
            .score(35)
            .label(ScoreLabel.WEAK)
            .weekReturn("-2.1%")
            .volumeChange("-12%")
            .risingStockRatio(35)
            .reasons(List.of("1주 수익률 -2.1%", "거래량 12% 감소", "상승 종목 비율 35%"))
            .stockCount(28)
            .build());
  }

  @Override
  public List<SectorScoreDto> getAllSectors() {
    return sectors;
  }

  @Override
  public List<StockInfo> getStocksBySectorId(String sectorId) {
    List<KrxStockItem> stocks = stocksBySector.getOrDefault(sectorId, List.of());
    return stocks.stream().map(StockInfo::fromKrxStockItem).toList();
  }

  @Override
  public List<KrxStockItem> getStocksBySectorName(String sectorName) {
    return stocksBySector.getOrDefault(sectorName, List.of());
  }

  private Map<String, List<KrxStockItem>> initMockStocks() {
    Map<String, List<KrxStockItem>> map = new HashMap<>();

    // 기술주
    map.put(
        "기술주",
        List.of(
            createMockStock("005930", "삼성전자", 78500, 1500, 1.95, "기술주", 523000000L),
            createMockStock("000660", "SK하이닉스", 198000, 4000, 2.06, "기술주", 144000000L),
            createMockStock("035420", "NAVER", 215000, -3000, -1.38, "기술주", 35000000L),
            createMockStock("035720", "카카오", 42500, 500, 1.19, "기술주", 18900000L),
            createMockStock("051910", "LG화학", 385000, 8000, 2.12, "기술주", 27200000L)));

    // 바이오
    map.put(
        "바이오",
        List.of(
            createMockStock("207940", "삼성바이오로직스", 890000, 15000, 1.71, "바이오", 59000000L),
            createMockStock("068270", "셀트리온", 178500, 2500, 1.42, "바이오", 24300000L),
            createMockStock("326030", "SK바이오팜", 89500, -1500, -1.65, "바이오", 7200000L)));

    // 금융
    map.put(
        "금융",
        List.of(
            createMockStock("105560", "KB금융", 78500, 1000, 1.29, "금융", 32600000L),
            createMockStock("055550", "신한지주", 51200, 700, 1.39, "금융", 26400000L),
            createMockStock("086790", "하나금융지주", 62300, 800, 1.30, "금융", 18700000L)));

    // 에너지
    map.put(
        "에너지",
        List.of(
            createMockStock("096770", "SK이노베이션", 125000, -500, -0.40, "에너지", 11700000L),
            createMockStock("010950", "S-Oil", 72500, 500, 0.69, "에너지", 8200000L)));

    // 소비재
    map.put(
        "소비재",
        List.of(
            createMockStock("051900", "LG생활건강", 385000, -2000, -0.52, "소비재", 6000000L),
            createMockStock("090430", "아모레퍼시픽", 128500, -1000, -0.77, "소비재", 7500000L)));

    // 자동차
    map.put(
        "자동차",
        List.of(
            createMockStock("005380", "현대차", 245000, -5000, -2.00, "자동차", 52400000L),
            createMockStock("000270", "기아", 128500, -2500, -1.91, "자동차", 52000000L),
            createMockStock("012330", "현대모비스", 245000, -3000, -1.21, "자동차", 23200000L)));

    return map;
  }

  private KrxStockItem createMockStock(
      String code,
      String name,
      int price,
      int priceChange,
      double changeRate,
      String sector,
      long marketCap) {
    return KrxStockItem.builder()
        .stockCode(code)
        .stockName(name)
        .marketType("KOSPI")
        .closingPrice(price)
        .priceChange(priceChange)
        .changeRate(changeRate)
        .marketCap(marketCap)
        .sectorName(sector)
        .build();
  }
}
