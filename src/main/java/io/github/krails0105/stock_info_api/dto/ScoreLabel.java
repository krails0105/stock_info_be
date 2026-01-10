package io.github.krails0105.stock_info_api.dto;

public enum ScoreLabel {
  STRONG, // 70~100
  NEUTRAL, // 40~69
  WEAK; // 0~39

  public static ScoreLabel fromScore(int score) {
    if (score >= 70) return STRONG;
    if (score >= 40) return NEUTRAL;
    return WEAK;
  }
}
