package io.github.krails0105.stock_info_api.dto.domain;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 섹터(업종) 기본 정보 도메인 DTO
 *
 * <p>업종별 집계 정보를 담는 핵심 도메인 객체
 */
@Getter
@Builder
public class Sector {

  /** 섹터 ID (예: "TECH", "BIO") */
  private String id;

  /** 섹터명 (예: "전기전자", "바이오") */
  private String name;

  /** 소속 종목 수 */
  private int stockCount;

  /** 평균 등락률 (%) */
  private double avgChangeRate;

  /** 상승 종목 수 */
  private int risingCount;

  /** 하락 종목 수 */
  private int fallingCount;

  /** 상승 종목 비율 (%) */
  private int risingRatio;

  /** 소속 종목 목록 (선택적) */
  private List<Stock> stocks;
}
