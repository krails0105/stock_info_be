package io.github.krails0105.stock_info_api.dto.insight;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockInsight {

  private final StockEntity entity;
  private final InsightMeta meta;
  private final InsightScore score;
  private final InsightSummary summary;
  private final InsightReasons reasons;
  private final InsightNews news;

  @Getter
  @Builder
  public static class StockEntity {
    private final String type = "STOCK";
    private final String code;
    private final String name;
    private final String sectorName;
  }
}
