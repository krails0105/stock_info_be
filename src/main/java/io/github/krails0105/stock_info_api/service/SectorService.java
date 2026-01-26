package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.HotSectorDto;
import io.github.krails0105.stock_info_api.dto.MarketSummaryDto;
import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.ScoreboardResponse;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import io.github.krails0105.stock_info_api.dto.response.StockListItem;
import io.github.krails0105.stock_info_api.provider.SectorDataProvider;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SectorService {

  /** P0-1: TOP3 후보 최소 표본 수 (5개 미만이면 TOP3에서 제외) */
  private static final int MIN_SAMPLE_SIZE_FOR_TOP3 = 5;

  private final SectorDataProvider sectorDataProvider;

  public ScoreboardResponse getScoreboard() {
    List<SectorScoreDto> allSectors = sectorDataProvider.getAllSectors();

    // 점수 기준 정렬
    List<SectorScoreDto> sortedSectors =
        allSectors.stream()
            .sorted(Comparator.comparingInt(SectorScoreDto::getScore).reversed())
            .toList();

    // P0-1: Hot Sectors TOP 3 - 표본 수 5개 이상인 섹터만 후보로 선정
    List<HotSectorDto> hotSectors =
        sortedSectors.stream()
            .filter(s -> s.getStockCount() >= MIN_SAMPLE_SIZE_FOR_TOP3)
            .limit(3)
            .map(this::toHotSector)
            .toList();

    // 시장 요약 계산
    MarketSummaryDto marketSummary = calculateMarketSummary(allSectors);

    return ScoreboardResponse.builder()
        .asOf(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
        .marketSummary(marketSummary)
        .hotSectors(hotSectors)
        .sectors(sortedSectors)
        .build();
  }

  public List<SectorScoreDto> getAllSectors() {
    return sectorDataProvider.getAllSectors().stream()
        .sorted(Comparator.comparingInt(SectorScoreDto::getScore).reversed())
        .toList();
  }

  /** 특정 섹터(업종)에 속한 종목 목록 조회 - Domain DTO 반환 */
  public List<StockInfo> getStocksBySectorId(String sectorId) {
    return sectorDataProvider.getStocksBySectorId(sectorId);
  }

  /**
   * 섹터별 종목 목록 조회
   *
   * @param sectorName 업종명 (예: "전기전자", "바이오")
   * @return 해당 업종에 속한 종목 목록 (StockListItem으로 변환)
   */
  public List<StockListItem> getStocksBySectorName(String sectorName) {
    List<KrxStockItem> krxStocks = sectorDataProvider.getStocksBySectorName(sectorName);
    return krxStocks.stream()
        .map(StockInfo::fromKrxStockItem)
        .map(StockListItem::fromStockInfo)
        .toList();
  }

  private HotSectorDto toHotSector(SectorScoreDto sector) {
    return HotSectorDto.builder()
        .sectorId(sector.getSectorId())
        .sectorName(sector.getSectorName())
        .score(sector.getScore())
        .label(sector.getLabel())
        .reasons(sector.getReasons())
        .build();
  }

  private MarketSummaryDto calculateMarketSummary(List<SectorScoreDto> sectors) {
    double avgScore = sectors.stream().mapToInt(SectorScoreDto::getScore).average().orElse(50);

    ScoreLabel label = ScoreLabel.fromScore((int) avgScore);
    String oneLiner = generateOneLiner(label, avgScore);

    return MarketSummaryDto.builder().label(label).oneLiner(oneLiner).build();
  }

  private String generateOneLiner(ScoreLabel label, double avgScore) {
    return switch (label) {
      case STRONG -> String.format("오늘 시장: 강세 (평균 점수 %.0f점)", avgScore);
      case NEUTRAL -> String.format("오늘 시장: 중립 (평균 점수 %.0f점)", avgScore);
      case WEAK -> String.format("오늘 시장: 약세 (평균 점수 %.0f점)", avgScore);
    };
  }
}
