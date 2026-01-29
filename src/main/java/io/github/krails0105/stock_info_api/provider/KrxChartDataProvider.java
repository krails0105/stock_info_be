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
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * KRX 차트 데이터 Provider (prod 프로파일)
 *
 * <p>TODO: 실제 KRX API 연동 시 이 클래스 구현
 */
@Component
@Profile("prod")
@Slf4j
public class KrxChartDataProvider implements ChartDataProvider {

  @Override
  public ChartResponse getChartData(String stockCode, String range) {
    log.debug("KRX chart data request: code={}, range={}", stockCode, range);

    // TODO: 실제 KRX API 연동
    // 현재는 Mock 데이터 반환
    List<ChartDataPoint> dataPoints = generateDataPoints(stockCode, range);

    return ChartResponse.builder()
        .stockCode(stockCode)
        .stockName(getStockName(stockCode))
        .range(range)
        .dataPoints(dataPoints)
        .meta(
            ChartMeta.builder()
                .asOf(OffsetDateTime.now(ZoneId.of("Asia/Seoul")))
                .source("KRX")
                .build())
        .build();
  }

  private String getStockName(String stockCode) {
    // TODO: 실제 종목명 조회
    return switch (stockCode) {
      case "005930" -> "삼성전자";
      case "000660" -> "SK하이닉스";
      case "035420" -> "NAVER";
      case "035720" -> "카카오";
      default -> stockCode;
    };
  }

  private List<ChartDataPoint> generateDataPoints(String stockCode, String range) {
    long basePrice = 50000L + Math.abs(stockCode.hashCode() % 150000);

    if ("1D".equals(range)) {
      return generateHourlyDataPoints(basePrice, stockCode);
    }
    return generateDailyDataPoints(basePrice, range, stockCode);
  }

  private List<ChartDataPoint> generateHourlyDataPoints(long basePrice, String stockCode) {
    List<ChartDataPoint> points = new ArrayList<>();
    Random random = new Random(basePrice + stockCode.hashCode());

    LocalDateTime today = LocalDate.now().atTime(9, 0);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    long currentPrice = basePrice;

    for (int i = 0; i < 14; i++) {
      LocalDateTime time = today.plusMinutes(i * 30L);

      double change = (random.nextDouble() - 0.5) * 0.02;
      currentPrice = (long) (currentPrice * (1 + change));
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
        date = endDate;
        if (i > 0) break;
      }

      double change = (random.nextDouble() - 0.5) * 0.06;
      currentPrice = (long) (currentPrice * (1 + change));
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
      case "1D" -> 14;
      case "1W" -> 7;
      case "1M" -> 22;
      case "3M" -> 66;
      case "1Y" -> 52;
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
