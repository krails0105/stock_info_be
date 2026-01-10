package io.github.krails0105.stock_info_api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockListResponse {
  private String sectorId;
  private String sectorName;
  private int sectorScore;
  private ScoreLabel sectorLabel;
  private List<StockScoreDto> stocks;
  private int totalCount;
}
