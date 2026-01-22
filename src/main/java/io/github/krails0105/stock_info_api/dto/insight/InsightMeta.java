package io.github.krails0105.stock_info_api.dto.insight;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InsightMeta {

  public enum Source {
    KRX,
    KIS,
    NAVER,
    DART,
    NEWS
  }

  private final LocalDateTime asOf;
  private final List<Source> sources;
  private final double coverage;
  private final int stalenessSec;
  @Builder.Default private final String disclaimerKey = "NOT_INVESTMENT_ADVICE";
}
