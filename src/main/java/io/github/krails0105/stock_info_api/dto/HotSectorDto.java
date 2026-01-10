package io.github.krails0105.stock_info_api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotSectorDto {
  private String sectorId;
  private String sectorName;
  private int score;
  private ScoreLabel label;
  private List<String> reasons;
}
