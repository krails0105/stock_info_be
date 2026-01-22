package io.github.krails0105.stock_info_api.service.rule;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.insight.InsightSummary.Template;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Importance;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Tag;
import io.github.krails0105.stock_info_api.dto.insight.ReasonCard;
import io.github.krails0105.stock_info_api.dto.insight.ReasonCard.Polarity;
import io.github.krails0105.stock_info_api.dto.insight.StockInsight;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RuleEngineService 테스트.
 *
 * <p>스펙 요구사항:
 * <ul>
 *   <li>룰 평가 순서: HF(하드필터) → M(과열/변동성) → N(뉴스) → P(실적) → V(밸류) → S(안정성)</li>
 *   <li>템플릿 선택: E(리스크) > B(모멘텀) > D(성장) > A(밸류) > C(안정)</li>
 *   <li>카드 슬롯: positive 2-3개, caution 1-2개, caution 최소 1개 보장</li>
 * </ul>
 */
class RuleEngineServiceTest {

  private RuleEngineService ruleEngineService;

  @BeforeEach
  void setUp() {
    ruleEngineService = new RuleEngineService();
  }

  // ==================== 기본 시그널 빌더 ====================

  private StockSignals.StockSignalsBuilder baseSignals() {
    return StockSignals.builder()
        .stockCode("005930")
        .stockName("삼성전자")
        .sectorName("전기전자")
        .changeRate(1.5)
        .dataCoverage(0.8);
  }

  // ==================== 하드 필터 테스트 (HF) ====================

  @Nested
  @DisplayName("하드 필터 (HF) 테스트")
  class HardFilterTests {

    @Test
    @DisplayName("HF-01: 거래정지 종목은 E_RISK 템플릿 + 경고 카드")
    void testSuspendedStock() {
      StockSignals signals = baseSignals()
          .isSuspended(true)
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.E_RISK);
      assertThat(insight.getReasons().getTriggeredRules()).contains("HF-01");
      assertThat(insight.getReasons().getCaution()).anyMatch(
          c -> c.getText().contains("거래정지"));
    }

    @Test
    @DisplayName("HF-02: 유동성 낮은 종목은 경고 카드 추가")
    void testLowLiquidity() {
      StockSignals signals = baseSignals()
          .liquidityScore(0.05) // < 0.1 → hasLowLiquidity() = true
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("HF-02");
      assertThat(insight.getReasons().getCaution()).anyMatch(
          c -> c.getText().contains("유동성"));
    }

    @Test
    @DisplayName("HF-03: PER 비정상(적자) 종목은 경고 카드 + 밸류 평가 스킵")
    void testValuationAnomaly() {
      StockSignals signals = baseSignals()
          .per(-5.0) // per <= 0 → hasValuationAnomaly() = true
          .sectorMedianPer(15.0)
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("HF-03");
      // V-01 (저평가)가 트리거되지 않아야 함 (밸류 평가 스킵)
      assertThat(insight.getReasons().getTriggeredRules()).doesNotContain("V-01");
    }

    @Test
    @DisplayName("HF-04: 데이터 커버리지 낮으면 경고 카드 + positive 카드 2개 제한")
    void testLowCoverage() {
      StockSignals signals = baseSignals()
          .dataCoverage(0.4) // 50% 미만
          .per(10.0)
          .sectorMedianPer(15.0)
          .earningsTrend("IMPROVING")
          .roe(15.0)
          .sectorMedianRoe(10.0)
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("HF-04");
      // positive 카드는 2개 이하
      assertThat(insight.getReasons().getPositive().size()).isLessThanOrEqualTo(2);
    }
  }

  // ==================== 과열/변동성 테스트 (M) ====================

  @Nested
  @DisplayName("과열/변동성 (M) 테스트")
  class OverheatVolatilityTests {

    @Test
    @DisplayName("M-04: 급등 + 거래량 폭증은 E_RISK 템플릿 + 과열 경고")
    void testOverheatWithVolumeSpike() {
      StockSignals signals = baseSignals()
          .changeRate(7.0) // 5% 초과 급등
          .volumeRatio(2.5) // 2.0 이상
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.E_RISK);
      assertThat(insight.getReasons().getTriggeredRules()).contains("M-04");
      assertThat(insight.getReasons().getCaution()).anyMatch(
          c -> c.getText().contains("급등") || c.getText().contains("과열"));
    }

    @Test
    @DisplayName("M-05: 급락은 경고 카드 추가")
    void testSharpDecline() {
      StockSignals signals = baseSignals()
          .changeRate(-6.0) // 5% 초과 급락
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("M-05");
      // 급락 시 caution 카드가 있어야 함 (급락/리스크/관리 관련 문구)
      assertThat(insight.getReasons().getCaution()).isNotEmpty();
    }
  }

  // ==================== 뉴스 테스트 (N) ====================

  @Nested
  @DisplayName("뉴스 (N) 테스트")
  class NewsTests {

    @Test
    @DisplayName("N-01: 긍정 뉴스(실적/수주)는 positive 카드 추가")
    void testPositiveNews() {
      NewsItem positiveNews = NewsItem.builder()
          .title("삼성전자 분기 실적 호조")
          .publisher("한경")
          .publishedAt(LocalDateTime.now())
          .url("https://example.com/news/1")
          .tags(List.of(Tag.EARNINGS))
          .importance(Importance.HIGH)
          .build();

      StockSignals signals = baseSignals()
          .newsItems(List.of(positiveNews))
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("N-01");
      assertThat(insight.getReasons().getPositive()).anyMatch(
          c -> c.getText().contains("긍정") || c.getText().contains("촉매"));
    }

    @Test
    @DisplayName("N-02: 부정 뉴스(규제 리스크)는 E_RISK 템플릿 + 경고 카드")
    void testNegativeNews() {
      NewsItem negativeNews = NewsItem.builder()
          .title("삼성전자 규제 리스크 부각")
          .publisher("매경")
          .publishedAt(LocalDateTime.now())
          .url("https://example.com/news/2")
          .tags(List.of(Tag.REGULATION_RISK))
          .importance(Importance.HIGH)
          .build();

      StockSignals signals = baseSignals()
          .newsItems(List.of(negativeNews))
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.E_RISK);
      assertThat(insight.getReasons().getTriggeredRules()).contains("N-02");
      assertThat(insight.getReasons().getCaution()).anyMatch(
          c -> c.getText().contains("리스크") || c.getText().contains("변동성"));
    }
  }

  // ==================== 실적/수익성 테스트 (P) ====================

  @Nested
  @DisplayName("실적/수익성 (P) 테스트")
  class FundamentalsTests {

    @Test
    @DisplayName("P-01: 실적 개선 추세는 D_GROWTH 템플릿 + positive 카드")
    void testImprovingEarnings() {
      StockSignals signals = baseSignals()
          .earningsTrend("IMPROVING")
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.D_GROWTH);
      assertThat(insight.getReasons().getTriggeredRules()).contains("P-01");
      assertThat(insight.getReasons().getPositive()).anyMatch(
          c -> c.getText().contains("실적"));
    }

    @Test
    @DisplayName("P-02: 실적 둔화는 경고 카드 추가")
    void testDecliningEarnings() {
      StockSignals signals = baseSignals()
          .earningsTrend("DECLINING")
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("P-02");
      // 실적 둔화 시 caution 카드가 있어야 함
      assertThat(insight.getReasons().getCaution()).isNotEmpty();
    }
  }

  // ==================== 밸류 테스트 (V) ====================

  @Nested
  @DisplayName("밸류 (V) 테스트")
  class ValuationTests {

    @Test
    @DisplayName("V-01: PER 저평가(섹터 대비 0.8배 이하)는 A_VALUE 템플릿 + positive 카드")
    void testUndervaluedPer() {
      StockSignals signals = baseSignals()
          .per(10.0)
          .sectorMedianPer(15.0) // 10/15 = 0.67 < 0.8
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.A_VALUE);
      assertThat(insight.getReasons().getTriggeredRules()).contains("V-01");
      assertThat(insight.getReasons().getPositive()).anyMatch(
          c -> c.getText().contains("PER") || c.getText().contains("부담"));
    }

    @Test
    @DisplayName("V-02: PER 고평가(섹터 대비 1.3배 이상)는 경고 카드 추가")
    void testOvervaluedPer() {
      StockSignals signals = baseSignals()
          .per(25.0)
          .sectorMedianPer(15.0) // 25/15 = 1.67 > 1.3
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getReasons().getTriggeredRules()).contains("V-02");
      assertThat(insight.getReasons().getCaution()).anyMatch(
          c -> c.getText().contains("기대") || c.getText().contains("조정"));
    }
  }

  // ==================== 안정성/모멘텀 테스트 (S/M) ====================

  @Nested
  @DisplayName("안정성/모멘텀 (S/M) 테스트")
  class StabilityMomentumTests {

    @Test
    @DisplayName("S-01: 변동성 낮음은 C_STABLE 템플릿 + positive 카드")
    void testLowVolatility() {
      StockSignals signals = baseSignals()
          .volatility(5.0)
          .sectorMedianVolatility(10.0) // 5/10 = 0.5 < 0.7
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.C_STABLE);
      assertThat(insight.getReasons().getTriggeredRules()).contains("S-01");
      assertThat(insight.getReasons().getPositive()).anyMatch(
          c -> c.getText().contains("변동성") || c.getText().contains("초보자"));
    }

    @Test
    @DisplayName("M-01: 거래량 증가(1.5배 이상)는 B_MOMENTUM 템플릿")
    void testVolumeIncrease() {
      StockSignals signals = baseSignals()
          .volumeRatio(1.8) // 1.5 이상
          .changeRate(2.0) // 급등 아님
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.B_MOMENTUM);
      assertThat(insight.getReasons().getTriggeredRules()).contains("M-01");
    }
  }

  // ==================== 카드 슬롯 규칙 테스트 ====================

  @Nested
  @DisplayName("카드 슬롯 규칙 테스트")
  class CardSlotTests {

    @Test
    @DisplayName("caution 카드 최소 1개 보장")
    void testMinimumCautionCard() {
      // 긍정적인 조건만 있는 경우
      StockSignals signals = baseSignals()
          .per(10.0)
          .sectorMedianPer(15.0) // 저평가
          .earningsTrend("IMPROVING") // 실적 개선
          .volatility(5.0)
          .sectorMedianVolatility(10.0) // 낮은 변동성
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      // caution 카드는 최소 1개
      assertThat(insight.getReasons().getCaution()).isNotEmpty();
      assertThat(insight.getReasons().getCaution().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("리스크 상황에서 caution 카드 2개")
    void testMaxCautionCards() {
      StockSignals signals = baseSignals()
          .changeRate(8.0) // 급등
          .volumeRatio(2.5) // 거래량 폭증 → 과열
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      // 과열 상황에서 caution 카드 2개
      assertThat(insight.getReasons().getCaution().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("positive 카드는 카테고리 다양성 유지")
    void testPositiveCardDiversity() {
      StockSignals signals = baseSignals()
          .per(10.0)
          .sectorMedianPer(15.0) // VALUATION
          .earningsTrend("IMPROVING") // FUNDAMENTALS
          .roe(20.0)
          .sectorMedianRoe(10.0) // FUNDAMENTALS
          .volatility(5.0)
          .sectorMedianVolatility(10.0) // STABILITY
          .volumeRatio(1.8) // MOMENTUM
          .dataCoverage(0.9)
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      // positive 카드는 3개까지, 다양한 카테고리
      List<ReasonCard> positives = insight.getReasons().getPositive();
      assertThat(positives.size()).isLessThanOrEqualTo(3);

      long distinctCategories = positives.stream()
          .map(ReasonCard::getCategory)
          .distinct()
          .count();
      // 최소 2개 이상의 다른 카테고리
      assertThat(distinctCategories).isGreaterThanOrEqualTo(2);
    }
  }

  // ==================== 템플릿 선택 우선순위 테스트 ====================

  @Nested
  @DisplayName("템플릿 선택 우선순위 테스트")
  class TemplateSelectionTests {

    @Test
    @DisplayName("E(리스크)가 B(모멘텀)보다 우선")
    void testRiskOverMomentum() {
      StockSignals signals = baseSignals()
          .volumeRatio(1.8) // 모멘텀 조건
          .isSuspended(true) // 리스크 조건
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.E_RISK);
    }

    @Test
    @DisplayName("B(모멘텀)가 D(성장)보다 우선")
    void testMomentumOverGrowth() {
      StockSignals signals = baseSignals()
          .volumeRatio(1.8) // 모멘텀 조건
          .earningsTrend("IMPROVING") // 성장 조건
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.B_MOMENTUM);
    }

    @Test
    @DisplayName("D(성장)가 A(밸류)보다 우선")
    void testGrowthOverValue() {
      StockSignals signals = baseSignals()
          .earningsTrend("IMPROVING") // 성장 조건
          .per(10.0)
          .sectorMedianPer(15.0) // 밸류 조건
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.D_GROWTH);
    }

    @Test
    @DisplayName("아무 조건도 없으면 C(안정) 템플릿")
    void testDefaultStable() {
      StockSignals signals = baseSignals().build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getSummary().getTemplate()).isEqualTo(Template.C_STABLE);
    }
  }

  // ==================== 점수/등급 테스트 ====================

  @Nested
  @DisplayName("점수/등급 테스트")
  class ScoreGradeTests {

    @Test
    @DisplayName("리스크 상황에서 점수 30 이하 제한")
    void testRiskScoreCap() {
      StockSignals signals = baseSignals()
          .changeRate(5.0) // 기본 높은 점수
          .isSuspended(true) // 리스크
          .build();

      StockInsight insight = ruleEngineService.buildStockInsight(signals);

      assertThat(insight.getScore().getValue()).isLessThanOrEqualTo(30);
      assertThat(insight.getScore().getGrade()).isEqualTo(ScoreLabel.WEAK);
    }

    @Test
    @DisplayName("데이터 커버리지에 따른 신뢰도")
    void testConfidenceByDataCoverage() {
      // 높은 커버리지
      StockSignals highCoverage = baseSignals()
          .dataCoverage(0.9)
          .build();
      StockInsight highInsight = ruleEngineService.buildStockInsight(highCoverage);
      assertThat(highInsight.getScore().getConfidence().name()).isEqualTo("HIGH");

      // 중간 커버리지
      StockSignals mediumCoverage = baseSignals()
          .dataCoverage(0.6)
          .build();
      StockInsight mediumInsight = ruleEngineService.buildStockInsight(mediumCoverage);
      assertThat(mediumInsight.getScore().getConfidence().name()).isEqualTo("MEDIUM");

      // 낮은 커버리지
      StockSignals lowCoverage = baseSignals()
          .dataCoverage(0.3)
          .build();
      StockInsight lowInsight = ruleEngineService.buildStockInsight(lowCoverage);
      assertThat(lowInsight.getScore().getConfidence().name()).isEqualTo("LOW");
    }
  }
}
