package io.github.krails0105.stock_info_api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailDto {
  private String code; // 종목 코드
  private String name; // 종목명
  private double price; // 현재가
  private double per; // PER
  private double pbr; // PBR
  private long volume; // 거래량
  private List<NewsDto> news; // 뉴스
}
