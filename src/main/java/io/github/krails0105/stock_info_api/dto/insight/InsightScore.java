package io.github.krails0105.stock_info_api.dto.insight;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InsightScore {

  public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
  }

  private final int value;
  private final ScoreLabel grade;
  private final Confidence confidence;
}
