package io.github.krails0105.stock_info_api.service.rule;

import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 룰 엔진에 전달되는 종목 신호 데이터. 기존 StockInfo/KrxStockFinancialItem에서 추출한 정보와 섹터 통계 정보를 포함. */
@Getter
@Builder
public class StockSignals {

  // === 기본 정보 ===
  private final String stockCode;
  private final String stockName;
  private final String sectorName;
  private final String market;

  // === 가격 정보 ===
  private final long price;
  private final long priceChange;
  private final double changeRate;

  // === Valuation 지표 ===
  private final Double per;
  private final Double pbr;
  private final Double forwardPer;

  // === 섹터 중앙값 (비교용) ===
  private final Double sectorMedianPer;
  private final Double sectorMedianPbr;
  private final Double sectorMedianRoe;
  private final Double sectorMedianVolatility;

  // === Fundamentals 지표 ===
  private final Double eps;
  private final Double bps;
  private final Double roe;
  private final Double dividendYield;

  // === 실적 트렌드 ===
  private final String earningsTrend; // IMPROVING, STABLE, DECLINING

  // === Momentum 지표 ===
  private final Double volumeRatio; // 최근 거래량 / 평균 거래량
  private final Double return5d; // 5일 수익률
  private final Double sectorReturn5dPercentile; // 섹터 내 5일 수익률 백분위

  // === Stability 지표 ===
  private final Double volatility; // 최근 변동성
  private final Long marketCap;
  private final Double liquidityScore; // 유동성 점수 (0~1)

  // === 상태 플래그 ===
  private final boolean isSuspended; // 거래정지 여부
  private final boolean isAdministrative; // 관리종목 여부
  private final boolean hasDeficit; // 적자 여부 (PER <= 0)

  // === 뉴스 ===
  private final List<NewsItem> newsItems;

  // === Coverage 정보 ===
  private final double dataCoverage; // 데이터 커버리지 (0~1)

  /** 적자 여부 판단 */
  public boolean hasValuationAnomaly() {
    return per == null || per <= 0;
  }

  /** 유동성 매우 낮음 판단 */
  public boolean hasLowLiquidity() {
    return liquidityScore != null && liquidityScore < 0.1;
  }
}
