package io.github.krails0105.stock_info_api.dto.response;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import lombok.Builder;
import lombok.Getter;

/**
 * 주식 목록 조회용 응답 DTO
 *
 * <p>프론트엔드 테이블 컬럼: # | 종목명 | 현재가 | 등락률 | 전일대비 | 시가총액 | PER | PBR | 점수
 */
@Getter
@Builder
public class StockListItem {

  /** 종목코드 */
  private String code;

  /** 종목명 */
  private String name;

  /** 현재가 */
  private long price;

  /** 전일대비 금액 (단위: 원) */
  private long priceChange;

  /** 등락률 (예: "+2.50%", "-1.30%") */
  private String changeRate;

  /** 시가총액 (단위: 원) */
  private Long marketCap;

  /** PER */
  private Double per;

  /** PBR */
  private Double pbr;

  /** 종합 점수 (0-100) */
  private int score;

  /** 점수 라벨 (STRONG/NEUTRAL/WEAK) */
  private ScoreLabel label;

  /** 시장구분 */
  private String market;

  /** 업종명 */
  private String sectorName;

  /** StockInfo 도메인 객체에서 변환 */
  public static StockListItem fromStockInfo(StockInfo stockInfo) {
    int score = calculateScore(stockInfo.getChangeRate());
    ScoreLabel label = ScoreLabel.fromScore(score);

    return StockListItem.builder()
        .code(stockInfo.getCode())
        .name(stockInfo.getName())
        .price(stockInfo.getPrice())
        .priceChange(stockInfo.getPriceChange())
        .changeRate(formatChangeRate(stockInfo.getChangeRate()))
        .marketCap(stockInfo.getMarketCap())
        .per(stockInfo.getPer())
        .pbr(stockInfo.getPbr())
        .score(score)
        .label(label)
        .market(stockInfo.getMarket())
        .sectorName(stockInfo.getSectorName())
        .build();
  }

  private static String formatChangeRate(double rate) {
    return String.format("%+.2f%%", rate);
  }

  private static int calculateScore(double changeRate) {
    int score = (int) ((changeRate + 5) * 10);
    return Math.max(0, Math.min(100, score));
  }
}
