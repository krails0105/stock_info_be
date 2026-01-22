package io.github.krails0105.stock_info_api.dto.insight;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsItem {

  public enum Tag {
    EARNINGS,
    CONTRACT,
    BUYBACK_DIVIDEND,
    REGULATION_RISK,
    MA,
    INDUSTRY,
    RUMOR
  }

  public enum Importance {
    HIGH,
    MEDIUM,
    LOW
  }

  private final String title;
  private final String publisher;
  private final LocalDateTime publishedAt;
  private final String url;
  private final List<Tag> tags;
  private final Importance importance;
  private final String clusterId;
}
