package io.github.krails0105.stock_info_api.service.rule;

/** 룰 엔진에서 사용하는 임계치 상수. 운영 데이터 기반으로 추후 튜닝 가능. */
public final class RuleConstants {

  private RuleConstants() {}

  // === Valuation 임계치 ===
  /** 섹터 대비 PER 저평가 기준 (0.8배 이하) */
  public static final double PER_LOW_RATIO = 0.8;

  /** 섹터 대비 PER 고평가 기준 (1.3배 이상) */
  public static final double PER_HIGH_RATIO = 1.3;

  /** 섹터 대비 PBR 저평가 기준 (0.8배 이하) */
  public static final double PBR_LOW_RATIO = 0.8;

  /** 섹터 대비 PBR 고평가 기준 (1.3배 이상) */
  public static final double PBR_HIGH_RATIO = 1.3;

  // === Momentum 임계치 ===
  /** 거래량 증가 기준 (1.5배 이상) */
  public static final double VOLUME_RATIO_THRESHOLD = 1.5;

  /** 과열 거래량 기준 (2.0배 이상) */
  public static final double VOLUME_RATIO_OVERHEAT = 2.0;

  /** 섹터 내 상위 수익률 기준 (상위 20%) */
  public static final double TOP_RETURN_PERCENTILE = 0.2;

  // === Stability 임계치 ===
  /** 섹터 대비 변동성 안정 기준 (0.8배 이하) */
  public static final double VOLATILITY_LOW_RATIO = 0.8;

  /** 섹터 대비 변동성 위험 기준 (1.3배 이상) */
  public static final double VOLATILITY_HIGH_RATIO = 1.3;

  // === Fundamentals 임계치 ===
  /** 섹터 대비 ROE 우수 기준 (1.2배 이상) */
  public static final double ROE_HIGH_RATIO = 1.2;

  // === Coverage 임계치 ===
  /** 정보 부족 판단 기준 (0.7 미만) */
  public static final double COVERAGE_LOW_THRESHOLD = 0.7;

  // === Card Limits ===
  /** 긍정 카드 최대 개수 */
  public static final int MAX_POSITIVE_CARDS = 3;

  /** 긍정 카드 최대 개수 (coverage 낮을 때) */
  public static final int MAX_POSITIVE_CARDS_LOW_COVERAGE = 2;

  /** 주의 카드 최소 개수 (초보자 보호) */
  public static final int MIN_CAUTION_CARDS = 1;

  /** 주의 카드 최대 개수 */
  public static final int MAX_CAUTION_CARDS = 2;

  // === Top Picks ===
  /** 기본 Top Picks 개수 */
  public static final int DEFAULT_TOP_PICKS_COUNT = 5;

  /** 뉴스 신선도 기준 (시간) */
  public static final int NEWS_FRESHNESS_HOURS = 72;

  // === Tone Matching 임계치 (P0-2) ===
  /** 강한 톤 점수 기준 (70점 이상) */
  public static final int TONE_STRONG_SCORE = 70;

  /** 강한 톤 coverage 기준 (0.7 이상) */
  public static final double TONE_STRONG_COVERAGE = 0.7;

  /** 중간 톤 점수 기준 (50점 이상) */
  public static final int TONE_MEDIUM_SCORE = 50;
}
