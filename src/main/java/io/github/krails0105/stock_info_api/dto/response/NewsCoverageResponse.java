package io.github.krails0105.stock_info_api.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 뉴스 커버리지 현황 응답 DTO.
 *
 * <p>종목/섹터별 뉴스 커버리지 지표를 제공한다.
 */
@Getter
@Builder
public class NewsCoverageResponse {

  /** 조회 시점 */
  private LocalDateTime asOf;

  /** 전체 종목 수 (뉴스 매핑된) */
  private int totalStocks;

  /** 24시간 내 뉴스 있는 종목 수 */
  private int stocksWithNews24h;

  /** 7일 내 뉴스 있는 종목 수 */
  private int stocksWithNews7d;

  /** 전체 섹터 수 */
  private int totalSectors;

  /** 24시간 내 뉴스 있는 섹터 수 */
  private int sectorsWithNews24h;

  /** 7일 내 뉴스 있는 섹터 수 */
  private int sectorsWithNews7d;

  /** 뉴스 0건 종목 목록 (7일 기준) */
  private List<String> zeroCoverageStocks;

  /** 뉴스 0건 섹터 목록 (7일 기준) */
  private List<String> zeroCoverageSectors;

  /** 종목별 커버리지 상세 (상위 20개) */
  private List<StockCoverage> stockCoverages;

  /** 섹터별 커버리지 상세 */
  private List<SectorCoverage> sectorCoverages;

  @Getter
  @Builder
  public static class StockCoverage {
    private String stockCode;
    private String stockName;
    private long newsCount24h;
    private long newsCount7d;
  }

  @Getter
  @Builder
  public static class SectorCoverage {
    private String sectorName;
    private long newsCount24h;
    private long newsCount7d;
  }
}
