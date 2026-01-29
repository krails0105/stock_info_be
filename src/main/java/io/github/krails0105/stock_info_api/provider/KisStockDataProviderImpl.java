package io.github.krails0105.stock_info_api.provider;

import static io.github.krails0105.stock_info_api.util.FormatUtils.calculateScoreFromChangeRate;
import static io.github.krails0105.stock_info_api.util.FormatUtils.formatChangeRate;
import static io.github.krails0105.stock_info_api.util.FormatUtils.getReturnGrade;

import io.github.krails0105.stock_info_api.config.KisRestClientProperties;
import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.kis.KisStockPriceResponse;
import io.github.krails0105.stock_info_api.service.KisTokenService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * KIS(한국투자증권) OpenAPI를 통한 주식 데이터 제공자
 *
 * <p>[KIS API 인증 헤더]
 *
 * <ul>
 *   <li>authorization: Bearer {토큰}
 *   <li>appkey: 앱 키
 *   <li>appsecret: 앱 시크릿
 *   <li>tr_id: 거래 ID (API마다 다름)
 * </ul>
 */
@Component
@Profile("dev") // prod 프로파일에서 KIS API 사용
@Slf4j
@RequiredArgsConstructor
public class KisStockDataProviderImpl implements StockDataProvider {

  private final RestClient kisRestClient;
  private final KisRestClientProperties props;
  private final KisTokenService kisTokenService;

  /** 거래 ID: 주식 현재가 조회 */
  private static final String TR_ID_STOCK_PRICE = "FHKST01010100";

  @Override
  public StockScoreDto getStockByCode(String code) {
    log.debug("KIS API 호출: 종목코드={}", code);

    String token = kisTokenService.getAccessToken();

    KisStockPriceResponse response =
        kisRestClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J") // J: 주식
                        .queryParam("FID_INPUT_ISCD", code) // 종목코드
                        .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header("appkey", props.getAppKey())
            .header("appsecret", props.getAppSecret())
            .header("tr_id", TR_ID_STOCK_PRICE)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(KisStockPriceResponse.class);

    if (response == null || response.getOutput() == null) {
      log.error("KIS API 응답 없음: code={}", code);
      return null;
    }

    if (!"0".equals(response.getResultCode())) {
      log.error("KIS API 오류: code={}, msg={}", response.getResultCode(), response.getMessage());
      return null;
    }

    return convertToStockScoreDto(code, response.getOutput());
  }

  /**
   * KIS API 응답을 StockScoreDto로 변환
   *
   * @param code 종목코드
   * @param output KIS API 응답 데이터
   * @return StockScoreDto
   */
  private StockScoreDto convertToStockScoreDto(String code, KisStockPriceResponse.Output output) {
    long price = parseLong(output.getCurrentPrice());
    double changeRate = parseDouble(output.getPriceChangeRate());

    // 점수 계산 (등락률 기반 간단 계산)
    int score = calculateScoreFromChangeRate(changeRate);
    ScoreLabel label = ScoreLabel.fromScore(score);

    String priceChangeStr = formatChangeRate(changeRate);

    return StockScoreDto.builder()
        .code(code)
        .name(output.getStockName())
        .score(score)
        .label(label)
        .price(price)
        .priceChange(priceChangeStr)
        .returnGrade(getReturnGrade(changeRate))
        .valuationGrade(getValuationGradeFromString(output.getPer(), output.getPbr()))
        .volumeGrade(getVolumeGrade(output.getAccumulatedVolume(), output.getPreviousVolume()))
        .reasons(
            List.of(
                "현재가 " + String.format("%,d", price) + "원",
                "등락률 " + priceChangeStr,
                "PER " + output.getPer() + " / PBR " + output.getPbr()))
        .build();
  }

  private String getValuationGradeFromString(String perStr, String pbrStr) {
    double per = parseDouble(perStr);
    double pbr = parseDouble(pbrStr);

    if (per > 0 && per < 10 && pbr < 1) return "저평가";
    if (per > 30 || pbr > 3) return "고평가";
    return "적정";
  }

  private String getVolumeGrade(String currentVol, String prevVol) {
    long current = parseLong(currentVol);
    long prev = parseLong(prevVol);

    if (prev == 0) return "보통";
    double ratio = (double) current / prev;

    if (ratio >= 2) return "급증";
    if (ratio >= 1.3) return "증가";
    if (ratio <= 0.5) return "감소";
    return "보통";
  }

  private long parseLong(String value) {
    if (value == null || value.isBlank()) return 0L;
    try {
      return Long.parseLong(value.replace(",", ""));
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private double parseDouble(String value) {
    if (value == null || value.isBlank()) return 0.0;
    try {
      return Double.parseDouble(value.replace(",", ""));
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  @Override
  public List<StockInfo> getAllStocks() {
    return List.of();
  }

  @Override
  public StockInfo getStockById(String stockId) {
    return null;
  }

  @Override
  public List<StockScoreDto> getStocksBySector(String sectorId) {
    // TODO: 업종별 종목 조회 구현
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<StockScoreDto> searchStocks(String keyword) {
    // TODO: 종목 검색 구현
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<StockScoreDto> getTopStocksBySector(String sectorId, int limit) {
    // TODO: 업종별 상위 종목 조회 구현
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
