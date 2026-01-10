package io.github.krails0105.stock_info_api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectorScoreDto {
  private String sectorId;
  private String sectorName;
  private int score;
  private ScoreLabel label;
  private String weekReturn; // 1주 수익률 (예: "+4.2%")
  private String volumeChange; // 거래량 변화 (예: "+28%")
  private int risingStockRatio; // 상승 종목 비율 (예: 65)
  private List<String> reasons;
  private int stockCount;
}
