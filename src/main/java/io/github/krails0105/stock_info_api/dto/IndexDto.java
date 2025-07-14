package io.github.krails0105.stock_info_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexDto {
  private String name; // 지수명 (KOSPI, KOSDAQ)
  private String currentPrice; // 현재 지수
  private String changePrice; // 변동가격
  private String changeRate; // 변동률
  private String changeDirection; // 상승/하락 방향
  private String updateTime; // 업데이트 시간
}
