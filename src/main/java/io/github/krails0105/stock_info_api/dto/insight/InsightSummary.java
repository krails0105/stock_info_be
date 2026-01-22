package io.github.krails0105.stock_info_api.dto.insight;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InsightSummary {

  public enum Template {
    A_VALUE,
    B_MOMENTUM,
    C_STABLE,
    D_GROWTH,
    E_RISK
  }

  public enum Tone {
    ACTIVE_GUIDE,
    CAUTIOUS_GUIDE
  }

  public enum FocusKey {
    EARNINGS_TREND,
    SECTOR_COMPARISON,
    VOLUME_TREND,
    VOLATILITY,
    NEXT_EARNINGS,
    NEWS_RISK
  }

  private final Template template;
  private final String headline;
  private final Tone tone;
  private final ActionHint actionHint;

  @Getter
  @Builder
  public static class ActionHint {
    private final String text;
    private final List<FocusKey> focusKeys;
  }
}
