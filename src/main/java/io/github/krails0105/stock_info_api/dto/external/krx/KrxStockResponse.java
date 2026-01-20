package io.github.krails0105.stock_info_api.dto.external.krx;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * KRX 주식 데이터 응답 DTO
 *
 * <p>KRX(한국거래소) CSV 다운로드 API 응답을 파싱한 결과
 *
 * <p>[CSV 컬럼 구조] 종목코드,종목명,시장구분,업종명,종가,대비,등락률,시가총액
 */
@Getter
@Builder
public class KrxStockResponse {

  private List<KrxStockItem> items;

  /** 업종명 기준으로 종목 그룹화 */
  public Map<String, List<KrxStockItem>> groupBySector() {
    return items.stream().collect(Collectors.groupingBy(KrxStockItem::getSectorName));
  }

  /** 개별 종목 데이터 */
  @Getter
  @Builder
  public static class KrxStockItem {

    /** 종목코드 (예: "005930") */
    private String stockCode;

    /** 종목명 (예: "삼성전자") */
    private String stockName;

    /** 시장구분 (예: "KOSPI", "KOSDAQ") */
    private String marketType;

    /** 업종명 (예: "전기전자", "IT") */
    private String sectorName;

    /** 종가 (단위: 원) */
    private long closingPrice;

    /** 대비 (전일 대비 가격 변동, 단위: 원) */
    private long priceChange;

    /** 등락률 (%, 예: 2.5, -1.3) */
    private double changeRate;

    /** 시가총액 (단위: 백만원 또는 원, KRX 응답에 따라 다름) */
    private long marketCap;

    /**
     * CSV 라인을 파싱하여 KrxStockItem 생성
     *
     * <p>업종명 등에 쉼표가 포함될 수 있어 쌍따옴표 안의 쉼표는 무시
     *
     * @param csvLine CSV 한 줄 (예: "005930","삼성전자","KOSPI","전기전자","72500","500","0.69","4500000")
     * @return KrxStockItem 객체
     */
    public static KrxStockItem fromCsvLine(String csvLine) {
      /*
       * CSV 파싱용 정규식: ,(?=(?:[^"]*"[^"]*")*[^"]*$)
       *
       * [문제 상황]
       * 일반 split(",")을 사용하면 쌍따옴표 안의 쉼표도 분리됨
       * 예: "식료품, 음료" → "식료품"와 " 음료"로 잘못 분리
       *
       * [정규식 분해 설명]
       *
       * ,                    → 쉼표를 찾음 (분리 기준점)
       * (?=                  → 전방탐색(lookahead) 시작: "쉼표 뒤에 이런 패턴이 있으면"
       *                         (실제로 소비하지 않고 조건만 확인)
       *
       *   (?:                → 비캡처 그룹 시작 (그룹화만 하고 결과에 포함 안함)
       *     [^"]*            → 쌍따옴표가 아닌 문자 0개 이상
       *     "                → 쌍따옴표 하나 (여는 따옴표)
       *     [^"]*            → 쌍따옴표가 아닌 문자 0개 이상
       *     "                → 쌍따옴표 하나 (닫는 따옴표)
       *   )*                 → 이 패턴(따옴표 쌍)이 0번 이상 반복
       *
       *   [^"]*              → 쌍따옴표가 아닌 문자 0개 이상
       *   $                  → 문자열 끝
       * )                    → 전방탐색 끝
       *
       * [핵심 원리]
       * "쉼표 뒤에 쌍따옴표가 짝수 개 있으면 분리, 홀수 개면 분리 안함"
       *
       * - 따옴표 밖의 쉼표 → 뒤에 따옴표가 짝수 개 (0, 2, 4...) → 분리 O
       * - 따옴표 안의 쉼표 → 뒤에 따옴표가 홀수 개 (1, 3, 5...) → 분리 X
       *
       * [예시]
       * 입력: "A","B, C","D"
       *
       * 첫번째 쉼표(A와 B 사이): 뒤에 따옴표 4개 (짝수) → 분리 O
       * 두번째 쉼표(B와 C 사이): 뒤에 따옴표 3개 (홀수) → 분리 X (따옴표 안)
       * 세번째 쉼표(C와 D 사이): 뒤에 따옴표 2개 (짝수) → 분리 O
       *
       * 결과: ["A"] ["B, C"] ["D"]
       */
      String[] fields = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      if (fields.length < 8) {
        throw new IllegalArgumentException("Invalid CSV format (expected 8 fields): " + csvLine);
      }

      return KrxStockItem.builder()
          .stockCode(cleanField(fields[0]))
          .stockName(cleanField(fields[1]))
          .marketType(cleanField(fields[2]))
          .sectorName(cleanField(fields[3]))
          .closingPrice(parseLong(fields[4]))
          .priceChange(parseLong(fields[5]))
          .changeRate(parseDouble(fields[6]))
          .marketCap(parseLong(fields[7]))
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
      return Long.parseLong(cleaned);
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
   * CSV 전체 응답을 파싱하여 KrxStockResponse 생성
   *
   * @param csvContent CSV 전체 내용 (헤더 포함)
   * @return KrxStockResponse 객체
   */
  public static KrxStockResponse fromCsv(String csvContent) {
    String[] lines = csvContent.split("\n");
    List<KrxStockItem> items =
        java.util.Arrays.stream(lines)
            .skip(1) // 헤더 스킵
            .filter(line -> !line.trim().isEmpty())
            .map(KrxStockItem::fromCsvLine)
            .toList();

    return KrxStockResponse.builder().items(items).build();
  }
}
