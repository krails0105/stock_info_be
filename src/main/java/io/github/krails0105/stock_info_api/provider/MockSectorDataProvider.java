package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
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

  public MockSectorDataProvider() {
    this.sectors = initMockData();
    this.sectorMap =
        sectors.stream()
            .collect(Collectors.toMap(SectorScoreDto::getSectorId, Function.identity()));
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
    // Mock: 빈 리스트 반환 (실제 데이터는 KrxSectorDataProviderImpl에서 제공)
    return List.of();
  }

  @Override
  public List<KrxStockItem> getStocksBySectorName(String sectorId) {
    return List.of();
  }
}
