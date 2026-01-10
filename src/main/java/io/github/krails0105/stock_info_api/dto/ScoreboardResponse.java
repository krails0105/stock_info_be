package io.github.krails0105.stock_info_api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreboardResponse {
  private OffsetDateTime asOf;
  private MarketSummaryDto marketSummary;
  private List<HotSectorDto> hotSectors; // TOP 3
  private List<SectorScoreDto> sectors; // 전체 섹터 리스트
}
