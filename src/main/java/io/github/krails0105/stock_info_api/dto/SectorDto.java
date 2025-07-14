package io.github.krails0105.stock_info_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorDto {
  private String sectorId; // 섹터 ID
  private String sectorName; // 섹터명
  private String description; // 섹터 설명
  private int interestScore; // 관심도 점수
  private int stockCount; // 섹터 내 종목 수
  private String avgPer; // 평균 PER
  private String avgPbr; // 평균 PBR
}
