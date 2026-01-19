package io.github.krails0105.stock_info_api.dto.external.krx;

import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * KRX 주식 재무지표 응답 DTO
 *
 * <p>KRX(한국거래소) CSV 다운로드 API 응답을 파싱한 결과
 *
 * <p>[CSV 컬럼 구조] 종목코드,종목명,종가,대비,등락률,EPS,PER,선행EPS,선행PER,BPS,PBR,주당배당금,배당수익률
 */
@Getter
@Builder
public class KrxStockFinancialResponse {

  private List<KrxStockFinancialItem> items;

  /** 종목코드를 key로, StockInfo를 value로 갖는 Map 반환 */
  public Map<String, StockInfo> groupByStockCode() {
    return items.stream()
        .map(StockInfo::fromKrxFinancialItem)
        .collect(Collectors.toMap(StockInfo::getCode, Function.identity()));
  }

  /** 개별 종목 재무지표 데이터 */
  @Getter
  @Builder
  public static class KrxStockFinancialItem {

    /** 종목코드 (예: "005930") */
    private String stockCode;

    /** 종목명 (예: "삼성전자") */
    private String stockName;

    /** 종가 (단위: 원) */
    private long closingPrice;

    /** 대비 (전일 대비 가격 변동, 단위: 원) */
    private long priceChange;

    /** 등락률 (%, 예: 2.5, -1.3) */
    private double changeRate;

    /** EPS - 주당순이익 (Earnings Per Share, 단위: 원) */
    private double eps;

    /** PER - 주가수익비율 (Price Earnings Ratio) */
    private double per;

    /** 선행 EPS - 예상 주당순이익 (Forward EPS, 단위: 원) */
    private double forwardEps;

    /** 선행 PER - 예상 주가수익비율 (Forward PER) */
    private double forwardPer;

    /** BPS - 주당순자산 (Book-value Per Share, 단위: 원) */
    private double bps;

    /** PBR - 주가순자산비율 (Price Book-value Ratio) */
    private double pbr;

    /** 주당배당금 (단위: 원) */
    private long dividendPerShare;

    /** 배당수익률 (%, 예: 2.1) */
    private double dividendYield;

    /**
     * CSV 라인을 파싱하여 KrxStockFinancialItem 생성
     *
     * @param csvLine CSV 한 줄
     * @return KrxStockFinancialItem 객체
     */
    public static KrxStockFinancialItem fromCsvLine(String csvLine) {
      String[] fields = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      if (fields.length < 13) {
        throw new IllegalArgumentException("Invalid CSV format (expected 13 fields): " + csvLine);
      }

      return KrxStockFinancialItem.builder()
          .stockCode(cleanField(fields[0]))
          .stockName(cleanField(fields[1]))
          .closingPrice(parseLong(fields[2]))
          .priceChange(parseLong(fields[3]))
          .changeRate(parseDouble(fields[4]))
          .eps(parseDouble(fields[5]))
          .per(parseDouble(fields[6]))
          .forwardEps(parseDouble(fields[7]))
          .forwardPer(parseDouble(fields[8]))
          .bps(parseDouble(fields[9]))
          .pbr(parseDouble(fields[10]))
          .dividendPerShare(parseLong(fields[11]))
          .dividendYield(parseDouble(fields[12]))
          .build();
    }

    private static String cleanField(String value) {
      return value.trim().replace("\"", "");
    }

    private static long parseLong(String value) {
      String cleaned = value.trim().replace("\"", "").replace(",", "");
      if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals("N/A")) {
        return 0L;
      }
      return Long.parseLong(cleaned);
    }

    private static double parseDouble(String value) {
      String cleaned = value.trim().replace("\"", "").replace(",", "");
      if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals("N/A")) {
        return 0.0;
      }
      return Double.parseDouble(cleaned);
    }
  }

  /**
   * CSV 전체 응답을 파싱하여 KrxStockFinancialResponse 생성
   *
   * @param csvContent CSV 전체 내용 (헤더 포함)
   * @return KrxStockFinancialResponse 객체
   */
  public static KrxStockFinancialResponse fromCsv(String csvContent) {
    String[] lines = csvContent.split("\n");
    List<KrxStockFinancialItem> items =
        java.util.Arrays.stream(lines)
            .skip(1) // 헤더 스킵
            .filter(line -> !line.trim().isEmpty())
            .map(KrxStockFinancialItem::fromCsvLine)
            .toList();

    return KrxStockFinancialResponse.builder().items(items).build();
  }
}
