package io.github.krails0105.stock_info_api.dto.insight;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InsightReasons {

  private final List<ReasonCard> positive;
  private final List<ReasonCard> caution;
  private final List<String> triggeredRules;
}
