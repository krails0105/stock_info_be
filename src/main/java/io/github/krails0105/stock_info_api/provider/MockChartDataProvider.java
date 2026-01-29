package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.response.ChartResponse;
import io.github.krails0105.stock_info_api.dto.response.ChartResponse.ChartDataPoint;
import io.github.krails0105.stock_info_api.dto.response.ChartResponse.ChartMeta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Mock 차트 데이터 Provider (local 프로파일) */
@Component
@Profile("local")
public class MockChartDataProvider implements ChartDataProvider {

  private static final Map<String, String> STOCK_NAMES =
      Map.of(
          "005930", "삼성전자",
          "000660", "SK하이닉스",
          "035420", "NAVER",
          "035720", "카카오",
          "051910", "LG화학");

  private static final Map<String, Long> BASE_PRICES =
      Map.of(
          "005930", 72000L,
          "000660", 185000L,
          "035420", 210000L,
          "035720", 45000L,
          "051910", 380000L);

  @Override
  public ChartResponse getChartData(String stockCode, String range) {
    String stockName = STOCK_NAMES.getOrDefault(stockCode, "알수없음");
    long basePrice = BASE_PRICES.getOrDefault(stockCode, 50000L);

    List<ChartDataPoint> dataPoints = generateDataPoints(basePrice, range, stockCode);

    return ChartResponse.builder()
        .stockCode(stockCode)
        .stockName(stockName)
        .range(range)
        .dataPoints(dataPoints)
        .meta(
            ChartMeta.builder()
                .asOf(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .source("MOCK")
                .build())
        .build();
  }

  private List<ChartDataPoint> generateDataPoints(long basePrice, String range, String stockCode) {
    // 1D는 시간별 데이터, 나머지는 일별 데이터
    if ("1D".equals(range)) {
      return generateHourlyDataPoints(basePrice, stockCode);
    }
    return generateDailyDataPoints(basePrice, range, stockCode);
  }

  /** 1D: 시간별 데이터 생성 (09:00 ~ 15:30 장중) */
  private List<ChartDataPoint> generateHourlyDataPoints(long basePrice, String stockCode) {
    List<ChartDataPoint> points = new ArrayList<>();
    Random random = new Random(basePrice + stockCode.hashCode());

    LocalDateTime today = LocalDate.now().atTime(9, 0); // 장 시작 09:00
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    long currentPrice = basePrice;

    // 09:00 ~ 15:00 (7시간, 30분 간격 = 14개 포인트)
    for (int i = 0; i < 14; i++) {
      LocalDateTime time = today.plusMinutes(i * 30L);

      // 가격 변동 (-1% ~ +1%)
      double change = (random.nextDouble() - 0.5) * 0.02;
      currentPrice = (long) (currentPrice * (1 + change));

      // 거래량 (10만 ~ 500만)
      long volume = 100_000L + random.nextLong(4_900_000L);

      points.add(
          ChartDataPoint.builder()
              .date(time.format(formatter))
              .price(currentPrice)
              .volume(volume)
              .build());
    }

    return points;
  }

  /** 일별 데이터 생성 (1W, 1M, 3M, 1Y) */
  private List<ChartDataPoint> generateDailyDataPoints(
      long basePrice, String range, String stockCode) {
    int count = getDataPointCount(range);
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = getStartDate(endDate, range);

    List<ChartDataPoint> points = new ArrayList<>();
    Random random = new Random(basePrice + stockCode.hashCode());

    long currentPrice = basePrice;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    int dayStep = Math.max(1, (int) Math.ceil((double) totalDays / (count - 1)));

    for (int i = 0; i < count; i++) {
      LocalDate date = startDate.plusDays((long) i * dayStep);

      if (date.isAfter(endDate)) {
        date = endDate; // 마지막 날짜는 오늘로 고정
        if (i > 0) break; // 중복 방지
      }

      // 가격 변동 (-3% ~ +3%)
      double change = (random.nextDouble() - 0.5) * 0.06;
      currentPrice = (long) (currentPrice * (1 + change));

      // 거래량 (100만 ~ 2000만)
      long volume = 1_000_000L + random.nextLong(19_000_000L);

      points.add(
          ChartDataPoint.builder()
              .date(date.format(formatter))
              .price(currentPrice)
              .volume(volume)
              .build());
    }

    return points;
  }

  private int getDataPointCount(String range) {
    return switch (range) {
      case "1D" -> 14; // 30분 간격 (장중 7시간)
      case "1W" -> 7; // 일별
      case "1M" -> 22; // 영업일 기준
      case "3M" -> 66; // 영업일 기준
      case "1Y" -> 52; // 주별
      default -> 22;
    };
  }

  private LocalDate getStartDate(LocalDate endDate, String range) {
    return switch (range) {
      case "1D" -> endDate;
      case "1W" -> endDate.minusWeeks(1);
      case "1M" -> endDate.minusMonths(1);
      case "3M" -> endDate.minusMonths(3);
      case "1Y" -> endDate.minusYears(1);
      default -> endDate.minusMonths(1);
    };
  }
}
