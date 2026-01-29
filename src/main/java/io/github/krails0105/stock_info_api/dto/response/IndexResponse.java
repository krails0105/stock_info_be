package io.github.krails0105.stock_info_api.dto.response;

import io.github.krails0105.stock_info_api.dto.domain.Index;
import io.github.krails0105.stock_info_api.util.FormatUtils;
import lombok.Builder;
import lombok.Getter;

/**
 * 지수 조회용 응답 DTO
 *
 * <p>클라이언트에 반환되는 가공된 지수 정보
 */
@Getter
@Builder
public class IndexResponse {

  /** 지수명 (예: "코스피", "코스닥") */
  private String name;

  /** 종가 (단위: 포인트) */
  private double closingPrice;

  /** 대비 (전일 대비 변동, 단위: 포인트) */
  private double priceChange;

  /** 등락률 (포맷팅된 문자열, 예: "+0.42%") */
  private String changeRate;

  /** 시장 상태 (상승/보합/하락) */
  private String marketStatus;

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

  /** Index 도메인 객체에서 변환 */
  public static IndexResponse fromIndex(Index index) {
    return IndexResponse.builder()
        .name(index.getName())
        .closingPrice(index.getClosingPrice())
        .priceChange(index.getPriceChange())
        .changeRate(FormatUtils.formatChangeRate(index.getChangeRate()))
        .marketStatus(FormatUtils.getMarketStatus(index.getChangeRate()))
        .openingPrice(index.getOpeningPrice())
        .highPrice(index.getHighPrice())
        .lowPrice(index.getLowPrice())
        .tradingVolume(index.getTradingVolume())
        .tradingValue(index.getTradingValue())
        .marketCap(index.getMarketCap())
        .build();
  }
}
