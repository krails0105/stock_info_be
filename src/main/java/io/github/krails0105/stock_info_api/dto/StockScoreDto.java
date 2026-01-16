package io.github.krails0105.stock_info_api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockScoreDto {
  private String code;
  private String name;
  private int score;
  private ScoreLabel label;
  private long price;
  private String priceChange; // 예: "+2.5%"

  // 세부 점수 (해석된 정보)
  private String returnGrade; // 최근 수익률 등급 (높음/보통/낮음)
  private String valuationGrade; // 밸류에이션 등급 (저평가/적정/고평가)
  private String volumeGrade; // 거래량 등급 (급증/증가/보통/감소)

  // 소속 섹터 정보
  private String sectorId;
  private String sectorName;
  private int sectorScore;

  // 이유 설명 (3줄)
  private List<String> reasons;
}
