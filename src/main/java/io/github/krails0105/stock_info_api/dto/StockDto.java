package io.github.krails0105.stock_info_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {
  private String stockCode; // 종목코드
  private String stockName; // 종목명
  private String sector; // 섹터
  private String currentPrice; // 현재가
  private String changePrice; // 변동가격
  private String changeRate; // 변동률
  private String changeDirection; // 상승/하락 방향
  private String volume; // 거래량
  private String per; // PER
  private String pbr; // PBR
  private String marketCap; // 시가총액
  private String dividend; // 배당률
  private String updateTime; // 업데이트 시간
}
