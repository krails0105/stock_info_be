package io.github.krails0105.stock_info_api.util;

/**
 * 포맷팅 및 계산 관련 유틸리티 클래스
 *
 * <p>Response DTO에서 공통으로 사용되는 포맷팅/계산 로직을 모아놓은 유틸리티
 */
public final class FormatUtils {

  private FormatUtils() {
    // 유틸 클래스 - 인스턴스화 방지
  }

  /**
   * 등락률을 포맷팅된 문자열로 변환
   *
   * @param rate 등락률 (예: 2.5, -1.3)
   * @return 포맷팅된 문자열 (예: "+2.50%", "-1.30%")
   */
  public static String formatChangeRate(double rate) {
    return String.format("%+.2f%%", rate);
  }

  /**
   * 등락률 기반 점수 계산 (-5% ~ +5% 범위를 0~100점으로 매핑)
   *
   * @param changeRate 등락률
   * @return 0~100 범위의 점수
   */
  public static int calculateScoreFromChangeRate(double changeRate) {
    int score = (int) ((changeRate + 5) * 10);
    return Math.max(0, Math.min(100, score));
  }

  /**
   * 등락률 기반 수익률 등급 계산
   *
   * @param changeRate 등락률
   * @return 등급 (높음/보통/낮음)
   */
  public static String getReturnGrade(double changeRate) {
    if (changeRate >= 3) return "높음";
    if (changeRate >= 0) return "보통";
    return "낮음";
  }

  /**
   * PER/PBR 기반 밸류에이션 등급 계산
   *
   * @param per PER 값
   * @param pbr PBR 값
   * @return 등급 (저평가/적정/고평가/정보없음)
   */
  public static String getValuationGrade(Double per, Double pbr) {
    if (per == null || pbr == null) return "정보없음";
    if (per > 0 && per < 10 && pbr < 1) return "저평가";
    if (per > 30 || pbr > 3) return "고평가";
    return "적정";
  }

  /**
   * 등락률 기반 시장 상태 계산
   *
   * @param changeRate 등락률
   * @return 상태 (상승/보합/하락)
   */
  public static String getMarketStatus(double changeRate) {
    if (changeRate > 0) return "상승";
    if (changeRate < 0) return "하락";
    return "보합";
  }
}
