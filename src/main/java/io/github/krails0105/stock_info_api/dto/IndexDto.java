package io.github.krails0105.stock_info_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexDto {
  private String name; // 지수명 (KOSPI, KOSDAQ)
  private double value; // 현재 지수
  private double change; // 등락
  private double changeRate; // 등락률
}
