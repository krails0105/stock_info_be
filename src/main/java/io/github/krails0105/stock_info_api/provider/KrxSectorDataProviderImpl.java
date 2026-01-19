package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
@Slf4j
public class KrxSectorDataProviderImpl implements SectorDataProvider {

  private final RestClient restClient;

  public KrxSectorDataProviderImpl() {
    this.restClient = RestClient.builder().build();
  }

  @Override
  public List<SectorScoreDto> getAllSectors() {
    KrxStockResponse krxResponse = fetchKrxData();
    return convertToSectorScores(krxResponse);
  }

  @Override
  public List<StockInfo> getStocksBySectorId(String sectorId) {
    KrxStockResponse krxResponse = fetchKrxData();
    Map<String, List<KrxStockItem>> sectorGroups = krxResponse.groupBySector();

    List<KrxStockItem> stocks = sectorGroups.getOrDefault(sectorId, List.of());
    return stocks.stream().map(StockInfo::fromKrxStockItem).toList();
  }

  /**
   * 섹터별 종목 목록 조회 (KRX 데이터 기반)
   *
   * @param sectorName 업종명 (예: "전기전자", "바이오")
   * @return 해당 업종에 속한 종목 목록
   */
  public List<KrxStockItem> getStocksBySectorName(String sectorName) {
    KrxStockResponse krxResponse = fetchKrxData();
    Map<String, List<KrxStockItem>> sectorGroups = krxResponse.groupBySector();
    return sectorGroups.getOrDefault(sectorName, List.of());
  }

  private KrxStockResponse fetchKrxData() {
    String code =
        "HDXDuwRT2eYe15H+LdVef5OacEuiDpZWQr/f/k5HMOURtSksuLS7Bnxpl86F7dAOkunw9BBwugQaSjGAcH15ed4UlmGP84YYw/wfb2rAlPYtBgM+EFJCxYg3zco1gIgRZqIo4cIzoURnTI8+MmkJ4v/rk8yudrOQ53ef0cNipdpCT2QuimcLoNhc1Lfcxcp2kuAKzXEa0IBpvpB7G2ws4c0zLiPvt4cWCSl6aep8ew2uIO5+TCBkSffs+tprQzXPvTCprTIXuXT9XxFb88awpQ==";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);

    byte[] responseBytes =
        restClient
            .post()
            .uri("https://data.krx.co.kr/comm/fileDn/download_csv/download.cmd")
            .header(
                HttpHeaders.REFERER, "http://data.krx.co.kr/comm/fileDn/GenerateOTP/generate.cmd")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(byte[].class);

    try {
      String csvContent = new String(Objects.requireNonNull(responseBytes), "EUC-KR");
      log.debug("KRX API Response: {}", csvContent);
      return KrxStockResponse.fromCsv(csvContent);
    } catch (Exception e) {
      log.error("Failed to parse KRX response", e);
      throw new RuntimeException("KRX 데이터 파싱 실패", e);
    }
  }

  private List<SectorScoreDto> convertToSectorScores(KrxStockResponse krxResponse) {
    Map<String, List<KrxStockItem>> sectorGroups = krxResponse.groupBySector();
    List<SectorScoreDto> sectors = new ArrayList<>();

    for (Map.Entry<String, List<KrxStockItem>> entry : sectorGroups.entrySet()) {
      String sectorName = entry.getKey();
      List<KrxStockItem> stocks = entry.getValue();

      int risingCount = (int) stocks.stream().filter(s -> s.getChangeRate() > 0).count();
      int risingRatio = stocks.isEmpty() ? 0 : (risingCount * 100) / stocks.size();

      double avgChangeRate =
          stocks.stream().mapToDouble(KrxStockItem::getChangeRate).average().orElse(0.0);

      int score = calculateScore(avgChangeRate, risingRatio);
      ScoreLabel label = ScoreLabel.fromScore(score);

      List<String> reasons = generateReasons(avgChangeRate, risingRatio, stocks.size());

      SectorScoreDto sectorDto =
          SectorScoreDto.builder()
              .sectorId(generateSectorId(sectorName))
              .sectorName(sectorName)
              .score(score)
              .label(label)
              .weekReturn(String.format("%+.2f%%", avgChangeRate))
              .volumeChange("-") // KRX 데이터에 거래량 변화 없음
              .risingStockRatio(risingRatio)
              .reasons(reasons)
              .stockCount(stocks.size())
              .build();

      sectors.add(sectorDto);
    }

    return sectors;
  }

  /**
   * 업종 점수 계산 (0~100점)
   *
   * <p>점수는 두 가지 지표를 50:50 비중으로 합산:
   *
   * <p>1. 평균 등락률 점수 (0~50점) - 업종 내 종목들의 평균 수익률 반영
   *
   * <p>2. 상승 종목 비율 점수 (0~50점) - 업종 내 상승 종목의 비율 반영
   *
   * <p>두 지표를 함께 사용하는 이유: - 평균 등락률만 보면 대형주 1개가 급등해도 점수가 높아짐 (왜곡 가능) - 상승 비율만 보면 소폭 상승이 많아도 점수가 높아짐
   * (강도 무시) - 둘을 조합하면 "얼마나 많은 종목이 + 얼마나 강하게" 상승했는지 종합 판단
   *
   * @param avgChangeRate 업종 내 평균 등락률 (%, 예: 2.5 = +2.5%)
   * @param risingRatio 상승 종목 비율 (%, 예: 70 = 70% 종목이 상승)
   * @return 0~100 사이의 점수
   */
  private int calculateScore(double avgChangeRate, int risingRatio) {

    /*
     * [평균 등락률 점수] - 최대 50점
     *
     * 공식: (avgChangeRate + 5) * 10
     * → -5%를 기준점(0점)으로 잡고, +5%를 만점(50점)으로 설정
     *
     * 계산 예시:
     *   -5% 이하 → 0점  (폭락장, 최악)
     *   -2.5%    → 25점 (하락장)
     *    0%      → 50점 (보합, 이미 절반 점수)
     *   +2.5%    → 75점 → 50점으로 제한 (상승장)
     *   +5% 이상 → 50점 (강세장, 만점)
     *
     * 왜 -5% ~ +5% 범위인가?
     * → 하루 업종 평균 등락률이 ±5%를 넘기는 경우는 극히 드묾
     * → 이 범위 내에서 점수를 세밀하게 분배하기 위함
     */
    int changeScore = (int) Math.min(Math.max((avgChangeRate + 5) * 10, 0), 50);

    /*
     * [상승 종목 비율 점수] - 최대 50점
     *
     * 공식: risingRatio / 2
     * → 상승 비율(0~100%)을 절반으로 나눠 0~50점으로 변환
     *
     * 계산 예시:
     *   0%   → 0점  (전 종목 하락)
     *   50%  → 25점 (절반 상승)
     *   100% → 50점 (전 종목 상승)
     *
     * 왜 단순히 절반으로 나누는가?
     * → 상승 비율은 이미 0~100% 범위로 정규화되어 있음
     * → 50점 만점에 맞추기 위해 /2 적용
     */
    int ratioScore = risingRatio / 2;

    /*
     * [최종 점수] = 등락률 점수 + 상승비율 점수
     *
     * 예시 시나리오:
     *   1) 강세장: 평균 +3%, 상승비율 80% → 50 + 40 = 90점 (STRONG)
     *   2) 보합장: 평균 0%, 상승비율 50%  → 50 + 25 = 75점 (STRONG)
     *   3) 약세장: 평균 -3%, 상승비율 30% → 20 + 15 = 35점 (WEAK)
     *   4) 폭락장: 평균 -5%, 상승비율 10% → 0 + 5 = 5점 (WEAK)
     */
    return Math.min(changeScore + ratioScore, 100);
  }

  private String generateSectorId(String sectorName) {
    return sectorName.replaceAll("\\s+", "_").toUpperCase();
  }

  private List<String> generateReasons(double avgChangeRate, int risingRatio, int stockCount) {
    List<String> reasons = new ArrayList<>();

    if (avgChangeRate > 2) {
      reasons.add("평균 등락률이 " + String.format("%.2f%%", avgChangeRate) + "로 강세");
    } else if (avgChangeRate < -2) {
      reasons.add("평균 등락률이 " + String.format("%.2f%%", avgChangeRate) + "로 약세");
    } else {
      reasons.add("평균 등락률 " + String.format("%.2f%%", avgChangeRate) + "로 보합세");
    }

    if (risingRatio >= 70) {
      reasons.add("상승 종목 비율 " + risingRatio + "%로 업종 전반 상승");
    } else if (risingRatio <= 30) {
      reasons.add("상승 종목 비율 " + risingRatio + "%로 업종 전반 하락");
    } else {
      reasons.add("상승 종목 비율 " + risingRatio + "%로 혼조세");
    }

    reasons.add("총 " + stockCount + "개 종목 포함");

    return reasons;
  }
}
