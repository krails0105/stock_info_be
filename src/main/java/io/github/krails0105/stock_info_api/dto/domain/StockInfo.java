package io.github.krails0105.stock_info_api.dto.domain;

import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockFinancialResponse.KrxStockFinancialItem;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import lombok.Builder;
import lombok.Getter;

/**
 * 주식 기본 정보 도메인 DTO
 *
 * <p>외부 API(KRX, KIS)에서 가져온 데이터를 내부에서 사용하는 표준 형식 비즈니스 로직에서 사용하는 핵심 도메인 객체
 */
@Getter
@Builder
public class StockInfo {

  /** 종목코드 (예: "005930") */
  private String code;

  /** 종목명 (예: "삼성전자") */
  private String name;

  /** 현재가/종가 (단위: 원) */
  private long price;

  /** 전일 대비 가격 변동 (단위: 원) */
  private long priceChange;

  /** 등락률 (%, 예: 2.5, -1.3) */
  private double changeRate;

  /** EPS - 주당순이익 (Earnings Per Share) */
  private Double eps;

  /** BPS - 주당순자산 (Book-value Per Share) */
  private Double bps;

  /** PER - 주가수익비율 (Price Earnings Ratio) */
  private Double per;

  /** PBR - 주가순자산비율 (Price Book-value Ratio) */
  private Double pbr;

  /** 선행 EPS - 예상 주당순이익 (Forward EPS) */
  private Double forwardEps;

  /** 선행 PER - 예상 주가수익비율 (Forward PER) */
  private Double forwardPer;

  /** 주당배당금 (단위: 원) */
  private Long dividendPerShare;

  /** 배당수익률 (%) */
  private Double dividendYield;

  /** 시장구분 (예: "KOSPI", "KOSDAQ") */
  private String market;

  /** 업종명 (예: "전기전자", "바이오") */
  private String sectorName;

  /** 시가총액 (단위: 원) */
  private Long marketCap;

  /**
   * KrxStockFinancialItem을 StockInfo로 변환
   *
   * @param item KRX 재무지표 데이터
   * @return StockInfo 객체
   */
  public static StockInfo fromKrxFinancialItem(KrxStockFinancialItem item) {
    return StockInfo.builder()
        .code(item.getStockCode())
        .name(item.getStockName())
        .price(item.getClosingPrice())
        .priceChange(item.getPriceChange())
        .changeRate(item.getChangeRate())
        .eps(item.getEps())
        .bps(item.getBps())
        .per(item.getPer())
        .pbr(item.getPbr())
        .forwardEps(item.getForwardEps())
        .forwardPer(item.getForwardPer())
        .dividendPerShare(item.getDividendPerShare())
        .dividendYield(item.getDividendYield())
        .build();
  }

  /**
   * KrxStockItem을 StockInfo로 변환
   *
   * @param item KRX 주식 데이터
   * @return StockInfo 객체
   */
  public static StockInfo fromKrxStockItem(KrxStockItem item) {
    return StockInfo.builder()
        .code(item.getStockCode())
        .name(item.getStockName())
        .price(item.getClosingPrice())
        .priceChange(item.getPriceChange())
        .changeRate(item.getChangeRate())
        .market(item.getMarketType())
        .sectorName(item.getSectorName())
        .marketCap(item.getMarketCap())
        .build();
  }

  /**
   * 재무 정보를 병합한 새 StockInfo 반환
   *
   * @param financialItem KRX 재무지표 데이터
   * @return 재무 정보가 병합된 StockInfo 객체
   */
  public StockInfo withFinancialInfo(KrxStockFinancialItem financialItem) {
    if (financialItem == null) {
      return this;
    }
    return StockInfo.builder()
        .code(this.code)
        .name(this.name)
        .price(this.price)
        .priceChange(this.priceChange)
        .changeRate(this.changeRate)
        .market(this.market)
        .sectorName(this.sectorName)
        .marketCap(this.marketCap)
        // 재무 정보 추가
        .eps(financialItem.getEps())
        .bps(financialItem.getBps())
        .per(financialItem.getPer())
        .pbr(financialItem.getPbr())
        .forwardEps(financialItem.getForwardEps())
        .forwardPer(financialItem.getForwardPer())
        .dividendPerShare(financialItem.getDividendPerShare())
        .dividendYield(financialItem.getDividendYield())
        .build();
  }
}
