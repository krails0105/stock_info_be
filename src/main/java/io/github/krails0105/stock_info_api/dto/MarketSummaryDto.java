package io.github.krails0105.stock_info_api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketSummaryDto {
  private ScoreLabel label;
  private String oneLiner;
}
