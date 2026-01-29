package io.github.krails0105.stock_info_api.dto.response;

import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import lombok.Builder;
import lombok.Getter;

/**
 * 종목 상세 조회 응답 DTO
 *
 * <p>프론트엔드 StockDetail 타입에 맞춘 응답 포맷 (KrxStockFinancialItem 기반)
 */
@Getter
@Builder
public class StockDetailResponse {

  // === 기본 정보 ===

  /** 종목코드 (예: "005930") */
  private String stockCode;

  /** 종목명 (예: "삼성전자") */
  private String stockName;

  /** 종가 (단위: 원) */
  private long closingPrice;

  /** 대비 (전일 대비 가격 변동, 단위: 원) */
  private long priceChange;

  /** 등락률 (%, 예: 2.5, -1.3) */
  private double changeRate;

  // === 수익 지표 ===

  /** EPS - 주당순이익 (Earnings Per Share, 단위: 원) */
  private double eps;

  /** PER - 주가수익비율 (Price Earnings Ratio) */
  private double per;

  /** 선행 EPS - 예상 주당순이익 (Forward EPS, 단위: 원) */
  private double forwardEps;

  /** 선행 PER - 예상 주가수익비율 (Forward PER) */
  private double forwardPer;

  // === 자산 지표 ===

  /** BPS - 주당순자산 (Book-value Per Share, 단위: 원) */
  private double bps;

  /** PBR - 주가순자산비율 (Price Book-value Ratio) */
  private double pbr;

  // === 배당 정보 ===

  /** 주당배당금 (단위: 원) */
  private long dividendPerShare;

  /** 배당수익률 (%, 예: 2.1) */
  private double dividendYield;

  /** StockInfo 도메인 객체에서 변환 */
  public static StockDetailResponse fromStockInfo(StockInfo stockInfo) {
    return StockDetailResponse.builder()
        .stockCode(stockInfo.getCode())
        .stockName(stockInfo.getName())
        .closingPrice(stockInfo.getPrice())
        .priceChange(stockInfo.getPriceChange())
        .changeRate(stockInfo.getChangeRate())
        .eps(stockInfo.getEps() != null ? stockInfo.getEps() : 0.0)
        .per(stockInfo.getPer() != null ? stockInfo.getPer() : 0.0)
        .forwardEps(stockInfo.getForwardEps() != null ? stockInfo.getForwardEps() : 0.0)
        .forwardPer(stockInfo.getForwardPer() != null ? stockInfo.getForwardPer() : 0.0)
        .bps(stockInfo.getBps() != null ? stockInfo.getBps() : 0.0)
        .pbr(stockInfo.getPbr() != null ? stockInfo.getPbr() : 0.0)
        .dividendPerShare(
            stockInfo.getDividendPerShare() != null ? stockInfo.getDividendPerShare() : 0L)
        .dividendYield(stockInfo.getDividendYield() != null ? stockInfo.getDividendYield() : 0.0)
        .build();
  }
}
