package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockFinancialResponse;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockFinancialResponse.KrxStockFinancialItem;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
@Slf4j
public class KrxStockDataProviderImpl implements StockDataProvider {

  private final RestClient restClient;

  public KrxStockDataProviderImpl() {
    this.restClient = RestClient.builder().build();
  }

  @Override
  public List<StockInfo> getAllStocks() {
    KrxStockFinancialResponse krxResponse = fetchKrxStockData();
    return krxResponse.getItems().stream().map(StockInfo::fromKrxFinancialItem).toList();
  }

  @Override
  public KrxStockFinancialItem getStocksByStockId(String stockId) {
    KrxStockFinancialResponse krxResponse = fetchKrxStockData();
    log.info(krxResponse.toString());
    return krxResponse.getItems().stream()
        .filter(s -> s.getStockCode().equals(stockId))
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<StockScoreDto> getStocksBySector(String sectorId) {
    return List.of();
  }

  @Override
  public StockScoreDto getStockByCode(String code) {
    return null;
  }

  @Override
  public List<StockScoreDto> searchStocks(String keyword) {
    return List.of();
  }

  @Override
  public List<StockScoreDto> getTopStocksBySector(String sectorId, int limit) {
    return List.of();
  }

  private KrxStockFinancialResponse fetchKrxStockData() {
    String code =
        "HDXDuwRT2eYe15H+LdVef2KPtJYOB4DNd0RiZfEw2X0RtSksuLS7Bnxpl86F7dAOLeq4x1yHv31Rs1BE2e3Ae6MM9dZFupZvytyVQZ9jrZnZvN2Hrce5tvIGLiR8s9y5B8OQ9d6t7s/rDB14nP4euh1EaJadcqRf9YjkQh0nKUA4fzZPS02rvBFmbYpTAvRGdwD7wum/aFW4tgK4ClLEJN5H+54DnIjVugDNM63c+O7XuZLf6HSF4XJ2vAxIHshN4+6Fn44l8zGYmDqMIVtilhdZx3Xdbl9EHo1GilYd0pFn7bMibk90Pcd6GSUpt3kRJW0OHp5SOJ36vltmMaa+pPlRlPUAtRyhXxw9N4xHMRSaP46lvhcuGI4r2zvdQk/X5AEGAxrvxGEeTSu7fcmLm7yUSXmUxqO8TTDyTesiy1Mof1EOegORxKB+S3Bm0h6kycQsztiES9OY9v/NyMlSHl8YLkWX26aHMuVmI7caumfchEVZ5OpuWIHm6PRejCcVnCHIKC13dsni0drKPL+rIFjtFpxqnm1GK3Z3Ny6hWpXVbO5S91neaAVNzKUq8sGi5WPj/15i4Te0eJD+lB04RVP1Uyv/Qg2DEQ1Yf+R9Q8qkCQmFR3QZ+Hhq0FD7iwy4m2QQb4/paPhuLCGWITS7KA==";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);

    byte[] responseBytes =
        restClient
            .post()
            .uri("https://data.krx.co.kr/comm/fileDn/download_csv/download.cmd")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(byte[].class);

    try {
      String csvContent = new String(Objects.requireNonNull(responseBytes), "EUC-KR");
      log.debug("KRX API Response: {}", csvContent);
      return KrxStockFinancialResponse.fromCsv(csvContent);
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
