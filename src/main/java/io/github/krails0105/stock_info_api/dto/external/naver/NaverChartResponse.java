package io.github.krails0105.stock_info_api.dto.external.naver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

/**
 * 네이버 금융 차트 API 응답 DTO.
 *
 * <p>네이버 금융 차트 API는 XML 형식으로 OHLCV 데이터를 반환한다. 형식: {@code <item data="yyyyMMdd|시가|고가|저가|종가|거래량" />}
 */
@Getter
@Builder
public class NaverChartResponse {

  private List<ChartItem> items;

  /** 개별 차트 데이터 아이템 */
  @Getter
  @Builder
  public static class ChartItem {
    /** 날짜 (yyyyMMdd 또는 yyyyMMddHHmm) */
    private String date;

    /** 시가 */
    private long openPrice;

    /** 고가 */
    private long highPrice;

    /** 저가 */
    private long lowPrice;

    /** 종가 */
    private long closePrice;

    /** 거래량 */
    private long volume;
  }

  /**
   * XML 응답을 파싱하여 NaverChartResponse 생성.
   *
   * @param xml 네이버 차트 API XML 응답
   * @return NaverChartResponse 객체
   */
  public static NaverChartResponse fromXml(String xml) {
    List<ChartItem> items = new ArrayList<>();

    // <item data="20260130|72000|72500|71500|72200|15000000" /> 패턴 매칭
    Pattern pattern = Pattern.compile("<item\\s+data=\"([^\"]+)\"");
    Matcher matcher = pattern.matcher(xml);

    while (matcher.find()) {
      String data = matcher.group(1);
      String[] fields = data.split("\\|");

      if (fields.length >= 6) {
        try {
          ChartItem item =
              ChartItem.builder()
                  .date(fields[0])
                  .openPrice(parseLong(fields[1]))
                  .highPrice(parseLong(fields[2]))
                  .lowPrice(parseLong(fields[3]))
                  .closePrice(parseLong(fields[4]))
                  .volume(parseLong(fields[5]))
                  .build();
          items.add(item);
        } catch (NumberFormatException e) {
          // 파싱 실패 시 해당 아이템 스킵
        }
      }
    }

    return NaverChartResponse.builder().items(items).build();
  }

  private static long parseLong(String value) {
    if (value == null || value.isEmpty()) {
      return 0L;
    }
    return Long.parseLong(value.trim());
  }
}
