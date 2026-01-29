package io.github.krails0105.stock_info_api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 종목 차트 데이터 응답 DTO */
@Getter
@Builder
public class ChartResponse {

  private String stockCode;
  private String stockName;
  private String range;
  private List<ChartDataPoint> dataPoints;
  private ChartMeta meta;

  @Getter
  @Builder
  public static class ChartDataPoint {
    private String date;
    private long price;
    private long volume;
  }

  @Getter
  @Builder
  public static class ChartMeta {
    private OffsetDateTime asOf;
    private String source;
  }
}
