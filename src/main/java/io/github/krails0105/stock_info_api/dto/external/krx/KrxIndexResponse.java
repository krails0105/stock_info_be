package io.github.krails0105.stock_info_api.dto.external.krx;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * KRX 지수 데이터 응답 DTO
 *
 * <p>KRX(한국거래소) CSV 다운로드 API 응답을 파싱한 결과
 *
 * <p>[CSV 컬럼 구조] 지수명,종가,대비,등락률,시가,고가,저가,거래량,거래대금,시가총액
 */
@Getter
@Builder
public class KrxIndexResponse {

  private List<KrxIndexItem> items;

  /** 개별 지수 데이터 */
  @Getter
  @Builder
  public static class KrxIndexItem {

    /** 지수명 (예: "코스피", "코스닥") */
    private String indexName;

    /** 종가 (단위: 포인트) */
    private double closingPrice;

    /** 대비 (전일 대비 변동, 단위: 포인트) */
    private double priceChange;

    /** 등락률 (%, 예: 2.5, -1.3) */
    private double changeRate;

    /** 시가 (단위: 포인트) */
    private double openingPrice;

    /** 고가 (단위: 포인트) */
    private double highPrice;

    /** 저가 (단위: 포인트) */
    private double lowPrice;

    /** 거래량 (단위: 주) */
    private long tradingVolume;

    /** 거래대금 (단위: 백만원) */
    private long tradingValue;

    /** 시가총액 (단위: 백만원) */
    private long marketCap;

    /**
     * CSV 라인을 파싱하여 KrxIndexItem 생성
     *
     * @param csvLine CSV 한 줄 (예:
     *     "코스피","2500.00","10.50","0.42","2490.00","2510.00","2485.00","500000","1000000","2000000")
     * @return KrxIndexItem 객체
     */
    public static KrxIndexItem fromCsvLine(String csvLine) {
      String[] fields = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      if (fields.length < 10) {
        throw new IllegalArgumentException("Invalid CSV format (expected 10 fields): " + csvLine);
      }

      return KrxIndexItem.builder()
          .indexName(cleanField(fields[0]))
          .closingPrice(parseDouble(fields[1]))
          .priceChange(parseDouble(fields[2]))
          .changeRate(parseDouble(fields[3]))
          .openingPrice(parseDouble(fields[4]))
          .highPrice(parseDouble(fields[5]))
          .lowPrice(parseDouble(fields[6]))
          .tradingVolume(parseLong(fields[7]))
          .tradingValue(parseLong(fields[8]))
          .marketCap(parseLong(fields[9]))
          .build();
    }

    private static String cleanField(String value) {
      return value.trim().replace("\"", "");
    }

    private static long parseLong(String value) {
      String cleaned = value.trim().replace("\"", "").replace(",", "");
      if (cleaned.isEmpty() || cleaned.equals("-")) {
        return 0L;
      }
      // KRX API에서 소수점 포함 숫자 (예: "600101.0")로 올 수 있어 double로 파싱 후 변환
      return (long) Double.parseDouble(cleaned);
    }

    private static double parseDouble(String value) {
      String cleaned = value.trim().replace("\"", "").replace(",", "");
      if (cleaned.isEmpty() || cleaned.equals("-")) {
        return 0.0;
      }
      return Double.parseDouble(cleaned);
    }
  }

  /**
   * CSV 전체 응답을 파싱하여 KrxIndexResponse 생성
   *
   * @param csvContent CSV 전체 내용 (헤더 포함)
   * @return KrxIndexResponse 객체
   */
  public static KrxIndexResponse fromCsv(String csvContent) {
    String[] lines = csvContent.split("\n");
    List<KrxIndexItem> items =
        java.util.Arrays.stream(lines)
            .skip(1) // 헤더 스킵
            .filter(line -> !line.trim().isEmpty())
            .map(KrxIndexItem::fromCsvLine)
            .toList();

    return KrxIndexResponse.builder().items(items).build();
  }
}
