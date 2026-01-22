package io.github.krails0105.stock_info_api.dto.insight;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReasonCard {

  public enum Category {
    VALUATION,
    FUNDAMENTALS,
    MOMENTUM,
    STABILITY,
    NEWS,
    RISK
  }

  public enum Polarity {
    POSITIVE,
    CAUTION
  }

  public enum Strength {
    WEAK,
    MEDIUM,
    STRONG
  }

  private final String key;
  private final Category category;
  private final Polarity polarity;
  private final String text;
  private final Strength strength;
  private final Map<String, Object> evidence;
}
