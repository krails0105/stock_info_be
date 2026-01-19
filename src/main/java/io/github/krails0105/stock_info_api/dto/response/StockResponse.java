package io.github.krails0105.stock_info_api.dto.response;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.domain.Stock;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 주식 상세 조회용 응답 DTO
 *
 * <p>점수, 등급, 분석 정보 등 상세 정보 포함
 */
@Getter
@Builder
public class StockResponse {

  /** 종목코드 */
  private String code;

  /** 종목명 */
  private String name;

  /** 현재가 */
  private long price;

  /** 등락률 (예: "+2.50%") */
  private String changeRate;

  /** 시장구분 */
  private String market;

  /** 업종명 */
  private String sectorName;

  /** 시가총액 */
  private long marketCap;

  /** 거래량 */
  private long volume;

  /** PER */
  private Double per;

  /** PBR */
  private Double pbr;

  // === 분석 정보 ===

  /** 종합 점수 (0-100) */
  private int score;

  /** 점수 라벨 (STRONG/NEUTRAL/WEAK) */
  private ScoreLabel label;

  /** 수익률 등급 (높음/보통/낮음) */
  private String returnGrade;

  /** 밸류에이션 등급 (저평가/적정/고평가) */
  private String valuationGrade;

  /** 거래량 등급 (급증/증가/보통/감소) */
  private String volumeGrade;

  /** 분석 이유 (3줄) */
  private List<String> reasons;

  /** Stock 도메인 객체에서 변환 (점수 정보 포함) */
  public static StockResponse from(Stock stock, int score, List<String> reasons) {
    ScoreLabel label = ScoreLabel.fromScore(score);

    return StockResponse.builder()
        .code(stock.getCode())
        .name(stock.getName())
        .price(stock.getPrice())
        .changeRate(formatChangeRate(stock.getChangeRate()))
        .market(stock.getMarket())
        .sectorName(stock.getSectorName())
        .marketCap(stock.getMarketCap())
        .volume(stock.getVolume())
        .per(stock.getPer())
        .pbr(stock.getPbr())
        .score(score)
        .label(label)
        .returnGrade(getReturnGrade(stock.getChangeRate()))
        .valuationGrade(getValuationGrade(stock.getPer(), stock.getPbr()))
        .volumeGrade("보통") // 기본값
        .reasons(reasons)
        .build();
  }

  private static String formatChangeRate(double rate) {
    return String.format("%+.2f%%", rate);
  }

  private static String getReturnGrade(double changeRate) {
    if (changeRate >= 3) return "높음";
    if (changeRate >= 0) return "보통";
    return "낮음";
  }

  private static String getValuationGrade(Double per, Double pbr) {
    if (per == null || pbr == null) return "정보없음";
    if (per > 0 && per < 10 && pbr < 1) return "저평가";
    if (per > 30 || pbr > 3) return "고평가";
    return "적정";
  }

  /** StockInfo 도메인 객체에서 변환 */
  public static StockResponse fromStockInfo(StockInfo stockInfo) {
    int score = calculateScore(stockInfo.getChangeRate());
    ScoreLabel label = ScoreLabel.fromScore(score);
    List<String> reasons = generateReasons(stockInfo);

    return StockResponse.builder()
        .code(stockInfo.getCode())
        .name(stockInfo.getName())
        .price(stockInfo.getPrice())
        .changeRate(formatChangeRate(stockInfo.getChangeRate()))
        .market(stockInfo.getMarket())
        .sectorName(stockInfo.getSectorName())
        .marketCap(stockInfo.getMarketCap() != null ? stockInfo.getMarketCap() : 0L)
        .volume(0L)
        .per(stockInfo.getPer())
        .pbr(stockInfo.getPbr())
        .score(score)
        .label(label)
        .returnGrade(getReturnGrade(stockInfo.getChangeRate()))
        .valuationGrade(getValuationGrade(stockInfo.getPer(), stockInfo.getPbr()))
        .volumeGrade("보통")
        .reasons(reasons)
        .build();
  }

  /** 등락률 기반 점수 계산 (-5% ~ +5% 범위를 0~100점으로 매핑) */
  private static int calculateScore(double changeRate) {
    int score = (int) ((changeRate + 5) * 10);
    return Math.max(0, Math.min(100, score));
  }

  /** StockInfo 기반 분석 이유 생성 */
  private static List<String> generateReasons(StockInfo stockInfo) {
    return List.of(
        String.format("등락률 %+.2f%%", stockInfo.getChangeRate()),
        stockInfo.getPer() != null ? String.format("PER %.2f", stockInfo.getPer()) : "PER 정보없음",
        stockInfo.getPbr() != null ? String.format("PBR %.2f", stockInfo.getPbr()) : "PBR 정보없음");
  }

  /** KrxStockItem에서 변환 */
  public static StockResponse fromKrxStockItem(KrxStockItem item) {
    int score = calculateScore(item.getChangeRate());
    ScoreLabel label = ScoreLabel.fromScore(score);

    return StockResponse.builder()
        .code(item.getStockCode())
        .name(item.getStockName())
        .price(item.getClosingPrice())
        .changeRate(formatChangeRate(item.getChangeRate()))
        .market(item.getMarketType())
        .sectorName(item.getSectorName())
        .marketCap(item.getMarketCap())
        .volume(0L)
        .per(null)
        .pbr(null)
        .score(score)
        .label(label)
        .returnGrade(getReturnGrade(item.getChangeRate()))
        .valuationGrade("정보없음")
        .volumeGrade("보통")
        .reasons(
            List.of(
                String.format("등락률 %+.2f%%", item.getChangeRate()),
                String.format("시가총액 %,d원", item.getMarketCap()),
                String.format("업종: %s", item.getSectorName())))
        .build();
  }
}
