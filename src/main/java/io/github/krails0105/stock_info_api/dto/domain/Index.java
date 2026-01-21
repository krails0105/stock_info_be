package io.github.krails0105.stock_info_api.dto.domain;

import io.github.krails0105.stock_info_api.dto.external.krx.KrxIndexResponse.KrxIndexItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Index {

  /** 지수명 (예: "코스피", "코스닥") */
  private String name;

  /** 종가 (단위: 포인트) */
  private double closingPrice;

  /** 대비 (전일 대비 변동, 단위: 포인트) */
  private double priceChange;

  /** 등락률 (%, 예: 2.5, -1.3) */
  private double changeRate;

  /** 시가 (단위: 포인트) */
  private double openingPrice;

  /** 고가 (단위: 포인트) */
  private double highPrice;

  /** 저가 (단위: 포인트) */
  private double lowPrice;

  /** 거래량 (단위: 주) */
  private long tradingVolume;

  /** 거래대금 (단위: 백만원) */
  private long tradingValue;

  /** 시가총액 (단위: 백만원) */
  private long marketCap;

  public static Index fromKrxIndexItem(KrxIndexItem item) {
    return Index.builder()
        .name(item.getIndexName())
        .closingPrice(item.getClosingPrice())
        .priceChange(item.getPriceChange())
        .changeRate(item.getChangeRate())
        .openingPrice(item.getOpeningPrice())
        .highPrice(item.getHighPrice())
        .lowPrice(item.getLowPrice())
        .tradingVolume(item.getTradingVolume())
        .tradingValue(item.getTradingValue())
        .marketCap(item.getMarketCap())
        .build();
  }
}
