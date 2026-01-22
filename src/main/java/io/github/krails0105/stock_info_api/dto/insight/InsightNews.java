package io.github.krails0105.stock_info_api.dto.insight;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InsightNews {

  private final List<String> issueBrief;
  private final List<NewsItem> headlineItems;
}
