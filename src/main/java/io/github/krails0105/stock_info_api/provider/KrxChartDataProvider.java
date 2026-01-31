package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.config.CacheConfig;
import io.github.krails0105.stock_info_api.dto.external.naver.NaverChartResponse;
import io.github.krails0105.stock_info_api.dto.external.naver.NaverChartResponse.ChartItem;
import io.github.krails0105.stock_info_api.dto.response.ChartResponse;
import io.github.krails0105.stock_info_api.dto.response.ChartResponse.ChartDataPoint;
import io.github.krails0105.stock_info_api.dto.response.ChartResponse.ChartMeta;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * KRX 차트 데이터 Provider (prod 프로파일).
 *
 * <p>네이버 금융 차트 API를 사용하여 실제 주가 데이터를 조회한다. Caffeine 캐시를 적용하여 60초 TTL로 외부 호출을 최소화한다.
 */
@Component
@Profile("prod")
@Slf4j
public class KrxChartDataProvider implements ChartDataProvider {

  private static final String NAVER_CHART_API_BASE = "https://fchart.stock.naver.com/siseJson.nhn";

  /** 주요 종목명 매핑 (차트 응답에 종목명 포함용) */
  private static final Map<String, String> STOCK_NAMES =
      Map.ofEntries(
          Map.entry("005930", "삼성전자"),
          Map.entry("000660", "SK하이닉스"),
          Map.entry("373220", "LG에너지솔루션"),
          Map.entry("207940", "삼성바이오로직스"),
          Map.entry("005380", "현대차"),
          Map.entry("000270", "기아"),
          Map.entry("068270", "셀트리온"),
          Map.entry("105560", "KB금융"),
          Map.entry("055550", "신한지주"),
          Map.entry("035420", "NAVER"),
          Map.entry("035720", "카카오"),
          Map.entry("005490", "포스코홀딩스"),
          Map.entry("012330", "현대모비스"),
          Map.entry("051910", "LG화학"),
          Map.entry("006400", "삼성SDI"),
          Map.entry("096770", "SK이노베이션"),
          Map.entry("028260", "삼성물산"),
          Map.entry("086790", "하나금융지주"),
          Map.entry("032830", "삼성생명"),
          Map.entry("066570", "LG전자"));

  private final RestClient restClient;

  public KrxChartDataProvider() {
    this.restClient =
        RestClient.builder()
            .defaultHeader("User-Agent", "Mozilla/5.0")
            .defaultHeader("Referer", "https://finance.naver.com")
            .build();
  }

  @Override
  @Cacheable(value = CacheConfig.CHART_CACHE, key = "#stockCode + '_' + #range")
  public ChartResponse getChartData(String stockCode, String range) {
    log.debug("Fetching chart data from Naver API: code={}, range={}", stockCode, range);

    try {
      String xml = fetchFromNaverApi(stockCode, range);
      NaverChartResponse naverResponse = NaverChartResponse.fromXml(xml);

      if (naverResponse.getItems().isEmpty()) {
        log.warn("No chart data returned from Naver API: code={}, range={}", stockCode, range);
        return buildEmptyResponse(stockCode, range);
      }

      List<ChartDataPoint> dataPoints = convertToDataPoints(naverResponse.getItems(), range);

      return ChartResponse.builder()
          .stockCode(stockCode)
          .stockName(getStockName(stockCode))
          .range(range)
          .dataPoints(dataPoints)
          .meta(
              ChartMeta.builder()
                  .asOf(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                  .source("NAVER")
                  .build())
          .build();

    } catch (RestClientException e) {
      log.error("Failed to fetch chart data from Naver API: code={}, range={}", stockCode, range);
      return buildEmptyResponse(stockCode, range);
    } catch (Exception e) {
      log.error(
          "Error processing chart data: code={}, range={}, error={}",
          stockCode,
          range,
          e.getMessage());
      return buildEmptyResponse(stockCode, range);
    }
  }

  /**
   * 네이버 금융 차트 API 호출.
   *
   * @param stockCode 종목 코드
   * @param range 기간
   * @return XML 응답 문자열
   */
  private String fetchFromNaverApi(String stockCode, String range) {
    ChartParams params = getChartParams(range);

    String url =
        String.format(
            "%s?symbol=%s&requestType=1&startTime=%s&endTime=%s&timeframe=%s&count=%d",
            NAVER_CHART_API_BASE,
            stockCode,
            params.startTime,
            params.endTime,
            params.timeframe,
            params.count);

    log.debug("Naver Chart API URL: {}", url);

    String response = restClient.get().uri(url).accept(MediaType.ALL).retrieve().body(String.class);

    log.debug("Naver Chart API response length: {}", response != null ? response.length() : 0);
    return response != null ? response : "";
  }

  /**
   * Range별 API 파라미터 계산.
   *
   * @param range 기간
   * @return 차트 파라미터
   */
  private ChartParams getChartParams(String range) {
    LocalDate today = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    String endTime = today.format(formatter);

    return switch (range) {
      case "1D" -> new ChartParams("minute", endTime, endTime, 78); // 5분봉 78개 (6.5시간)
      case "1W" -> new ChartParams("day", today.minusDays(7).format(formatter), endTime, 7);
      case "1M" -> new ChartParams("day", today.minusMonths(1).format(formatter), endTime, 30);
      case "3M" -> new ChartParams("day", today.minusMonths(3).format(formatter), endTime, 90);
      case "1Y" -> new ChartParams("week", today.minusYears(1).format(formatter), endTime, 52);
      default -> new ChartParams("day", today.minusMonths(1).format(formatter), endTime, 30);
    };
  }

  /**
   * NaverChartResponse 데이터를 ChartDataPoint 리스트로 변환.
   *
   * @param items 네이버 차트 아이템 리스트
   * @param range 기간
   * @return ChartDataPoint 리스트
   */
  private List<ChartDataPoint> convertToDataPoints(List<ChartItem> items, String range) {
    List<ChartDataPoint> dataPoints = new ArrayList<>();

    for (ChartItem item : items) {
      String formattedDate = formatDate(item.getDate(), range);

      dataPoints.add(
          ChartDataPoint.builder()
              .date(formattedDate)
              .price(item.getClosePrice())
              .volume(item.getVolume())
              .build());
    }

    // 시간순 정렬 (오래된 것 먼저)
    Collections.sort(dataPoints, (a, b) -> a.getDate().compareTo(b.getDate()));

    return dataPoints;
  }

  /**
   * 날짜 형식 변환.
   *
   * @param rawDate 원본 날짜 (yyyyMMdd 또는 yyyyMMddHHmm)
   * @param range 기간
   * @return 포맷된 날짜 문자열
   */
  private String formatDate(String rawDate, String range) {
    if (rawDate == null || rawDate.isEmpty()) {
      return "";
    }

    try {
      if ("1D".equals(range) && rawDate.length() >= 12) {
        // 분봉: yyyyMMddHHmm -> yyyy-MM-dd HH:mm
        return String.format(
            "%s-%s-%s %s:%s",
            rawDate.substring(0, 4),
            rawDate.substring(4, 6),
            rawDate.substring(6, 8),
            rawDate.substring(8, 10),
            rawDate.substring(10, 12));
      } else if (rawDate.length() >= 8) {
        // 일봉/주봉: yyyyMMdd -> yyyy-MM-dd
        return String.format(
            "%s-%s-%s", rawDate.substring(0, 4), rawDate.substring(4, 6), rawDate.substring(6, 8));
      }
    } catch (Exception e) {
      log.debug("Date format error: {}", rawDate);
    }

    return rawDate;
  }

  private String getStockName(String stockCode) {
    return STOCK_NAMES.getOrDefault(stockCode, stockCode);
  }

  private ChartResponse buildEmptyResponse(String stockCode, String range) {
    return ChartResponse.builder()
        .stockCode(stockCode)
        .stockName(getStockName(stockCode))
        .range(range)
        .dataPoints(List.of())
        .meta(
            ChartMeta.builder()
                .asOf(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .source("NAVER")
                .build())
        .build();
  }

  /** 차트 API 파라미터 */
  private record ChartParams(String timeframe, String startTime, String endTime, int count) {}
}
