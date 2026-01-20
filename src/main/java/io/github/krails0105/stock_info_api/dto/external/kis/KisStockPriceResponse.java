package io.github.krails0105.stock_info_api.dto.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * ============================================================================
 * KIS 주식 현재가 조회 응답 DTO
 * ============================================================================
 *
 * [KIS 주식현재가 API]
 * 종목코드로 현재 시세 정보를 조회하는 API
 *
 * [요청 예시]
 * GET /uapi/domestic-stock/v1/quotations/inquire-price
 *     ?FID_COND_MRKT_DIV_CODE=J          // 시장 구분 (J: 주식)
 *     &FID_INPUT_ISCD=005930             // 종목코드 (삼성전자)
 *
 * Headers:
 * - authorization: Bearer {토큰}
 * - appkey: {앱키}
 * - appsecret: {앱시크릿}
 * - tr_id: FHKST01010100                 // 거래ID (현재가 조회)
 *
 * [응답 예시]
 * {
 *   "rt_cd": "0",              // 성공 코드 (0: 성공)
 *   "msg_cd": "MCA00000",
 *   "msg1": "정상처리 되었습니다",
 *   "output": {
 *     "stck_prpr": "72500",    // 현재가
 *     "prdy_vrss": "500",      // 전일 대비
 *     "prdy_ctrt": "0.69",     // 전일 대비율
 *     ...
 *   }
 * }
 *
 * [중첩 클래스를 사용하는 이유]
 * API 응답 구조가 계층적(output 안에 실제 데이터)이기 때문
 * 별도 파일로 분리해도 되지만, 응답 전용이므로 내부 클래스로 그룹화
 */
@Getter
@NoArgsConstructor
public class KisStockPriceResponse {

  /*
   * 응답 코드
   * - "0": 정상 처리
   * - 그 외: 오류 (msg1 필드에서 상세 내용 확인)
   */
  @JsonProperty("rt_cd")
  private String resultCode;

  /*
   * 메시지 코드
   * - 예: "MCA00000" (정상)
   * - 에러 추적 시 활용
   */
  @JsonProperty("msg_cd")
  private String messageCode;

  /*
   * 응답 메시지
   * - 예: "정상처리 되었습니다"
   * - 에러 시 원인 설명이 담김
   */
  @JsonProperty("msg1")
  private String message;

  /*
   * 실제 시세 데이터
   * - JSON의 "output" 객체를 Output 클래스로 매핑
   */
  @JsonProperty("output")
  private Output output;

  /*
   * ========================================================================
   * 주식 시세 상세 정보 (내부 클래스)
   * ========================================================================
   *
   * [KIS API 필드 네이밍 규칙]
   * - stck_: 주식(stock) 관련
   * - prdy_: 전일(previous day) 관련
   * - acml_: 누적(accumulated) 관련
   */
  @Getter
  @NoArgsConstructor
  public static class Output { // static 내부 클래스: 외부 클래스 인스턴스 없이 생성 가능

    /*
     * 주식 현재가 (stck_prpr = stock present price)
     * - 단위: 원
     * - 문자열로 오므로 사용 시 숫자 변환 필요
     */
    @JsonProperty("stck_prpr")
    private String currentPrice;

    /*
     * 전일 대비 (prdy_vrss = previous day versus)
     * - 전일 종가 대비 등락 금액
     * - 양수: 상승, 음수: 하락
     */
    @JsonProperty("prdy_vrss")
    private String priceChange;

    /*
     * 전일 대비율 (prdy_ctrt = previous day contrast rate)
     * - 전일 종가 대비 등락률 (%)
     * - 예: "2.5" = 2.5% 상승
     */
    @JsonProperty("prdy_ctrt")
    private String priceChangeRate;

    /*
     * 누적 거래량 (acml_vol = accumulated volume)
     * - 당일 장 시작부터 현재까지의 총 거래량
     */
    @JsonProperty("acml_vol")
    private String accumulatedVolume;

    /*
     * 전일 거래량
     * - 어제 하루 전체 거래량
     * - 거래량 증감 계산에 사용
     */
    @JsonProperty("prdy_vol")
    private String previousVolume;

    /*
     * 당일 최고가 (stck_hgpr = stock high price)
     */
    @JsonProperty("stck_hgpr")
    private String highPrice;

    /*
     * 당일 최저가 (stck_lwpr = stock low price)
     */
    @JsonProperty("stck_lwpr")
    private String lowPrice;

    /*
     * 시가 (stck_oprc = stock open price)
     * - 당일 장 시작 가격
     */
    @JsonProperty("stck_oprc")
    private String openPrice;

    /*
     * PER (Price Earnings Ratio, 주가수익비율)
     * - 계산: 주가 / 주당순이익(EPS)
     * - 낮을수록 저평가 (일반적으로 10 이하면 저평가)
     * - 업종마다 기준이 다름
     */
    @JsonProperty("per")
    private String per;

    /*
     * PBR (Price Book-value Ratio, 주가순자산비율)
     * - 계산: 주가 / 주당순자산(BPS)
     * - 1 이하면 자산가치 대비 저평가
     */
    @JsonProperty("pbr")
    private String pbr;

    /*
     * 종목명 (hts_kor_isnm = HTS Korean issue name)
     * - HTS: Home Trading System (증권사 트레이딩 시스템)
     * - 예: "삼성전자"
     */
    @JsonProperty("hts_kor_isnm")
    private String stockName;
  }
}
