package io.github.krails0105.stock_info_api.dto.response;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import lombok.Builder;
import lombok.Getter;

/**
 * 주식 목록 조회용 응답 DTO
 *
 * <p>프론트엔드 테이블 컬럼: # | 종목명 | 점수 | 현재가 | 등락률
 */
@Getter
@Builder
public class StockListItem {

  /** 종목코드 */
  private String code;

  /** 종목명 */
  private String name;

  /** 종합 점수 (0-100) */
  private int score;

  /** 점수 라벨 (STRONG/NEUTRAL/WEAK) */
  private ScoreLabel label;

  /** 현재가 */
  private long price;

  /** 등락률 (예: "+2.50%", "-1.30%") */
  private String changeRate;

  /** 시장구분 */
  private String market;

  /** 업종명 */
  private String sectorName;
}
