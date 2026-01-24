package io.github.krails0105.stock_info_api.service.rule;

import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.COVERAGE_LOW_THRESHOLD;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.MAX_CAUTION_CARDS;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.MAX_POSITIVE_CARDS;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.MAX_POSITIVE_CARDS_LOW_COVERAGE;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.MIN_CAUTION_CARDS;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.PBR_HIGH_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.PBR_LOW_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.PER_HIGH_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.PER_LOW_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.ROE_HIGH_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.TONE_MEDIUM_SCORE;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.TONE_STRONG_COVERAGE;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.TONE_STRONG_SCORE;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.TOP_RETURN_PERCENTILE;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.VOLATILITY_HIGH_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.VOLATILITY_LOW_RATIO;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.VOLUME_RATIO_OVERHEAT;
import static io.github.krails0105.stock_info_api.service.rule.RuleConstants.VOLUME_RATIO_THRESHOLD;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import io.github.krails0105.stock_info_api.dto.insight.InsightMeta;
import io.github.krails0105.stock_info_api.dto.insight.InsightMeta.Source;
import io.github.krails0105.stock_info_api.dto.insight.InsightNews;
import io.github.krails0105.stock_info_api.dto.insight.InsightReasons;
import io.github.krails0105.stock_info_api.dto.insight.InsightScore;
import io.github.krails0105.stock_info_api.dto.insight.InsightScore.Confidence;
import io.github.krails0105.stock_info_api.dto.insight.InsightSummary;
import io.github.krails0105.stock_info_api.dto.insight.InsightSummary.ActionHint;
import io.github.krails0105.stock_info_api.dto.insight.InsightSummary.FocusKey;
import io.github.krails0105.stock_info_api.dto.insight.InsightSummary.Template;
import io.github.krails0105.stock_info_api.dto.insight.InsightSummary.Tone;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Importance;
import io.github.krails0105.stock_info_api.dto.insight.ReasonCard;
import io.github.krails0105.stock_info_api.dto.insight.ReasonCard.Category;
import io.github.krails0105.stock_info_api.dto.insight.ReasonCard.Polarity;
import io.github.krails0105.stock_info_api.dto.insight.ReasonCard.Strength;
import io.github.krails0105.stock_info_api.dto.insight.StockInsight;
import io.github.krails0105.stock_info_api.dto.insight.StockInsight.StockEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 종목 인사이트를 생성하는 룰 엔진 서비스. 스펙 문서의 룰 평가 순서와 카드 슬롯 규칙을 따름.
 *
 * <p>평가 순서: 1. 하드 리스크 (상태/유동성/적자/지표비정상/결측) 2. 과열/변동성 3. 뉴스 리스크 4. 실적/수익성 5. 밸류 (섹터 대비) 6. 안정성
 *
 * <p>템플릿 선택 우선순위: E(리스크) > B(모멘텀) > D(성장) > A(밸류) > C(안정)
 */
@Slf4j
@Service
public class RuleEngineService {

  /**
   * 종목 신호를 기반으로 StockInsight를 생성
   *
   * @param signals 종목 신호 데이터
   * @return 생성된 StockInsight
   */
  public StockInsight buildStockInsight(StockSignals signals) {
    List<String> triggeredRules = new ArrayList<>();
    List<ReasonCard> positiveCards = new ArrayList<>();
    List<ReasonCard> cautionCards = new ArrayList<>();
    RuleFlags flags = new RuleFlags();

    // === Phase A: Hard Filters ===
    evaluateHardFilters(signals, triggeredRules, cautionCards, flags);

    // === Phase B: Overheat/Volatility ===
    evaluateOverheatVolatility(signals, triggeredRules, cautionCards, flags);

    // === Phase C: News ===
    evaluateNews(signals, triggeredRules, positiveCards, cautionCards, flags);

    // === Phase D: Fundamentals ===
    evaluateFundamentals(signals, triggeredRules, positiveCards, cautionCards);

    // === Phase E: Valuation (guarded) ===
    if (!flags.hasValuationAnomaly) {
      evaluateValuation(signals, triggeredRules, positiveCards, cautionCards);
    }

    // === Phase F: Stability ===
    evaluateStability(signals, triggeredRules, positiveCards, cautionCards);

    // === Build Result ===
    Template template = pickTemplate(triggeredRules, signals, flags);

    // 점수 먼저 계산 (톤 매칭에 필요)
    int baseScore = calculateBaseScore(signals);
    // 리스크 플래그에 따른 조정
    if (flags.riskLevel == RiskLevel.HIGH) {
      baseScore = Math.min(baseScore, 30);
    } else if (flags.hasOverheat || flags.hasNegativeNews) {
      baseScore = Math.min(baseScore, 50);
    }

    InsightSummary summary = buildSummary(template, signals, flags, baseScore);

    // 카드 선택 및 정렬
    List<ReasonCard> finalPositive = selectPositiveCards(positiveCards, signals.getDataCoverage());
    List<ReasonCard> finalCaution = selectCautionCards(cautionCards, flags);

    // 최소 caution 1개 보장
    if (finalCaution.isEmpty()) {
      finalCaution.add(buildDefaultCaution());
    }

    InsightReasons reasons =
        InsightReasons.builder()
            .positive(finalPositive)
            .caution(finalCaution)
            .triggeredRules(triggeredRules)
            .build();

    InsightScore score = buildScore(baseScore, signals, flags);
    InsightMeta meta = buildMeta(signals);
    InsightNews news = buildNews(signals.getNewsItems());

    return StockInsight.builder()
        .entity(
            StockEntity.builder()
                .code(signals.getStockCode())
                .name(signals.getStockName())
                .sectorName(signals.getSectorName())
                .build())
        .meta(meta)
        .score(score)
        .summary(summary)
        .reasons(reasons)
        .news(news)
        .build();
  }

  // ==================== Phase A: Hard Filters ====================

  private void evaluateHardFilters(
      StockSignals signals,
      List<String> triggeredRules,
      List<ReasonCard> cautionCards,
      RuleFlags flags) {

    // HF-01: 거래정지/관리/상폐
    if (signals.isSuspended() || signals.isAdministrative()) {
      triggeredRules.add("HF-01");
      flags.topPicksEligible = false;
      flags.riskLevel = RiskLevel.HIGH;
      cautionCards.add(
          ReasonCard.builder()
              .key("HF-01")
              .category(Category.RISK)
              .polarity(Polarity.CAUTION)
              .text("거래정지 또는 관리종목 상태로 주의가 필요해요.")
              .strength(Strength.STRONG)
              .build());
    }

    // HF-02: 유동성 매우 낮음
    if (signals.hasLowLiquidity()) {
      triggeredRules.add("HF-02");
      cautionCards.add(
          ReasonCard.builder()
              .key("HF-02")
              .category(Category.RISK)
              .polarity(Polarity.CAUTION)
              .text("유동성이 매우 낮아 거래에 어려움이 있을 수 있어요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // HF-03: PER ≤ 0 또는 비정상
    if (signals.hasValuationAnomaly()) {
      triggeredRules.add("HF-03");
      flags.hasValuationAnomaly = true;
      cautionCards.add(
          ReasonCard.builder()
              .key("HF-03")
              .category(Category.RISK)
              .polarity(Polarity.CAUTION)
              .text("PER이 비정상이라 적자 가능성을 확인해 보세요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // HF-04: 결측 과다
    if (signals.getDataCoverage() < COVERAGE_LOW_THRESHOLD) {
      triggeredRules.add("HF-04");
      flags.hasLowCoverage = true;
      cautionCards.add(
          ReasonCard.builder()
              .key("HF-04")
              .category(Category.RISK)
              .polarity(Polarity.CAUTION)
              .text("분석에 필요한 정보가 일부 부족해요.")
              .strength(Strength.WEAK)
              .build());
    }
  }

  // ==================== Phase B: Overheat/Volatility ====================

  private void evaluateOverheatVolatility(
      StockSignals signals,
      List<String> triggeredRules,
      List<ReasonCard> cautionCards,
      RuleFlags flags) {

    // M-04: 급등 + volumeRatio ≥ 2.0
    if (signals.getVolumeRatio() != null
        && signals.getVolumeRatio() >= VOLUME_RATIO_OVERHEAT
        && signals.getChangeRate() > 5.0) {
      triggeredRules.add("M-04");
      flags.hasOverheat = true;
      cautionCards.add(
          ReasonCard.builder()
              .key("M-04")
              .category(Category.MOMENTUM)
              .polarity(Polarity.CAUTION)
              .text("급등 + 거래량 폭증이라 조정이 와도 이상하지 않은 구간이에요.")
              .strength(Strength.STRONG)
              .build());
    }

    // M-05: 급락/변동성 급증
    if (signals.getChangeRate() < -5.0
        || (signals.getVolatility() != null
            && signals.getSectorMedianVolatility() != null
            && signals.getVolatility() > signals.getSectorMedianVolatility() * VOLATILITY_HIGH_RATIO
            && signals.getChangeRate() < 0)) {
      triggeredRules.add("M-05");
      cautionCards.add(
          ReasonCard.builder()
              .key("M-05")
              .category(Category.MOMENTUM)
              .polarity(Polarity.CAUTION)
              .text("급락/변동이 커져 리스크 관리가 필요해요.")
              .strength(Strength.STRONG)
              .build());
    }

    // S-02: 변동성 높음
    if (signals.getVolatility() != null
        && signals.getSectorMedianVolatility() != null
        && signals.getVolatility() >= signals.getSectorMedianVolatility() * VOLATILITY_HIGH_RATIO) {
      triggeredRules.add("S-02");
      cautionCards.add(
          ReasonCard.builder()
              .key("S-02")
              .category(Category.STABILITY)
              .polarity(Polarity.CAUTION)
              .text("변동성이 큰 편이라 초보자에겐 부담일 수 있어요.")
              .strength(Strength.MEDIUM)
              .build());
    }
  }

  // ==================== Phase C: News ====================

  private void evaluateNews(
      StockSignals signals,
      List<String> triggeredRules,
      List<ReasonCard> positiveCards,
      List<ReasonCard> cautionCards,
      RuleFlags flags) {

    List<NewsItem> news = signals.getNewsItems();
    if (news == null || news.isEmpty()) {
      return;
    }

    // 중요도 HIGH 뉴스 분류
    List<NewsItem> highImportanceNews =
        news.stream().filter(n -> n.getImportance() == Importance.HIGH).toList();

    boolean hasHighNegative =
        highImportanceNews.stream()
            .anyMatch(n -> n.getTags().contains(NewsItem.Tag.REGULATION_RISK));

    boolean hasHighPositive =
        highImportanceNews.stream()
            .anyMatch(
                n ->
                    n.getTags().contains(NewsItem.Tag.EARNINGS)
                        || n.getTags().contains(NewsItem.Tag.CONTRACT)
                        || n.getTags().contains(NewsItem.Tag.BUYBACK_DIVIDEND));

    // N-02: 부정 뉴스
    if (hasHighNegative) {
      triggeredRules.add("N-02");
      flags.hasNegativeNews = true;
      String tag =
          highImportanceNews.stream()
              .filter(n -> n.getTags().contains(NewsItem.Tag.REGULATION_RISK))
              .flatMap(n -> n.getTags().stream())
              .findFirst()
              .map(Enum::name)
              .orElse("리스크");

      cautionCards.add(
          ReasonCard.builder()
              .key("N-02")
              .category(Category.NEWS)
              .polarity(Polarity.CAUTION)
              .text(String.format("%s 리스크 이슈가 있어 변동성 확대 가능성이 있어요.", tag))
              .strength(Strength.STRONG)
              .build());
    }

    // N-01: 긍정 뉴스
    if (hasHighPositive && !hasHighNegative) {
      triggeredRules.add("N-01");
      String tag =
          highImportanceNews.stream()
              .flatMap(n -> n.getTags().stream())
              .filter(
                  t ->
                      t == NewsItem.Tag.EARNINGS
                          || t == NewsItem.Tag.CONTRACT
                          || t == NewsItem.Tag.BUYBACK_DIVIDEND)
              .findFirst()
              .map(Enum::name)
              .orElse("긍정");

      positiveCards.add(
          ReasonCard.builder()
              .key("N-01")
              .category(Category.NEWS)
              .polarity(Polarity.POSITIVE)
              .text(String.format("최근 %s 긍정 이슈가 있어 단기 촉매가 될 수 있어요.", tag))
              .strength(Strength.MEDIUM)
              .build());
    }
  }

  // ==================== Phase D: Fundamentals ====================

  private void evaluateFundamentals(
      StockSignals signals,
      List<String> triggeredRules,
      List<ReasonCard> positiveCards,
      List<ReasonCard> cautionCards) {

    // P-01: 최근 실적 개선
    if ("IMPROVING".equals(signals.getEarningsTrend())) {
      triggeredRules.add("P-01");
      positiveCards.add(
          ReasonCard.builder()
              .key("P-01")
              .category(Category.FUNDAMENTALS)
              .polarity(Polarity.POSITIVE)
              .text("최근 실적 흐름이 좋아 후보로 올려둘 근거가 있어요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // P-02: 실적 둔화
    if ("DECLINING".equals(signals.getEarningsTrend())) {
      triggeredRules.add("P-02");
      cautionCards.add(
          ReasonCard.builder()
              .key("P-02")
              .category(Category.FUNDAMENTALS)
              .polarity(Polarity.CAUTION)
              .text("실적이 둔화되는 신호가 있어 보수적 접근이 좋아요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // P-03: ROE 우수
    if (signals.getRoe() != null
        && signals.getSectorMedianRoe() != null
        && signals.getRoe() >= signals.getSectorMedianRoe() * ROE_HIGH_RATIO) {
      triggeredRules.add("P-03");
      positiveCards.add(
          ReasonCard.builder()
              .key("P-03")
              .category(Category.FUNDAMENTALS)
              .polarity(Polarity.POSITIVE)
              .text("섹터 대비 수익성이 좋아 체력이 있는 편이에요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // P-05: 적자/마진 악화 (hasDeficit)
    if (signals.isHasDeficit()) {
      triggeredRules.add("P-05");
      cautionCards.add(
          ReasonCard.builder()
              .key("P-05")
              .category(Category.FUNDAMENTALS)
              .polarity(Polarity.CAUTION)
              .text("수익성 악화 가능성이 있어 리스크 확인이 우선이에요.")
              .strength(Strength.MEDIUM)
              .build());
    }
  }

  // ==================== Phase E: Valuation ====================

  private void evaluateValuation(
      StockSignals signals,
      List<String> triggeredRules,
      List<ReasonCard> positiveCards,
      List<ReasonCard> cautionCards) {

    Double per = signals.getPer();
    Double pbr = signals.getPbr();
    Double sectorMedianPer = signals.getSectorMedianPer();
    Double sectorMedianPbr = signals.getSectorMedianPbr();

    // V-01: PER 저평가
    if (per != null
        && sectorMedianPer != null
        && per > 0
        && per <= sectorMedianPer * PER_LOW_RATIO) {
      triggeredRules.add("V-01");
      positiveCards.add(
          ReasonCard.builder()
              .key("V-01")
              .category(Category.VALUATION)
              .polarity(Polarity.POSITIVE)
              .text("섹터 대비 PER이 낮아 가격 부담이 덜한 편이에요.")
              .strength(Strength.MEDIUM)
              .evidence(Map.of("per", per, "sectorMedianPer", sectorMedianPer))
              .build());
    }

    // V-02: PER 고평가
    if (per != null && sectorMedianPer != null && per >= sectorMedianPer * PER_HIGH_RATIO) {
      triggeredRules.add("V-02");
      cautionCards.add(
          ReasonCard.builder()
              .key("V-02")
              .category(Category.VALUATION)
              .polarity(Polarity.CAUTION)
              .text("기대가 많이 반영된 구간이라 실적이 빗나가면 조정 폭이 커질 수 있어요.")
              .strength(Strength.MEDIUM)
              .evidence(Map.of("per", per, "sectorMedianPer", sectorMedianPer))
              .build());
    }

    // V-03: PBR 저평가
    if (pbr != null
        && sectorMedianPbr != null
        && pbr > 0
        && pbr <= sectorMedianPbr * PBR_LOW_RATIO) {
      triggeredRules.add("V-03");
      positiveCards.add(
          ReasonCard.builder()
              .key("V-03")
              .category(Category.VALUATION)
              .polarity(Polarity.POSITIVE)
              .text("섹터 대비 PBR이 낮아 과열된 기대치가 아닌 편이에요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // V-04: PBR 고평가
    if (pbr != null && sectorMedianPbr != null && pbr >= sectorMedianPbr * PBR_HIGH_RATIO) {
      triggeredRules.add("V-04");
      cautionCards.add(
          ReasonCard.builder()
              .key("V-04")
              .category(Category.VALUATION)
              .polarity(Polarity.CAUTION)
              .text("프리미엄이 큰 편이라 성장/실적 확인이 중요해요.")
              .strength(Strength.MEDIUM)
              .build());
    }
  }

  // ==================== Phase F: Stability ====================

  private void evaluateStability(
      StockSignals signals,
      List<String> triggeredRules,
      List<ReasonCard> positiveCards,
      List<ReasonCard> cautionCards) {

    // S-01: 변동성 낮음
    if (signals.getVolatility() != null
        && signals.getSectorMedianVolatility() != null
        && signals.getVolatility() <= signals.getSectorMedianVolatility() * VOLATILITY_LOW_RATIO) {
      triggeredRules.add("S-01");
      positiveCards.add(
          ReasonCard.builder()
              .key("S-01")
              .category(Category.STABILITY)
              .polarity(Polarity.POSITIVE)
              .text("섹터 대비 변동성이 낮아 초보자에게 부담이 적은 편이에요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // S-03: 규모/유동성 상위
    if (signals.getLiquidityScore() != null && signals.getLiquidityScore() > 0.7) {
      triggeredRules.add("S-03");
      positiveCards.add(
          ReasonCard.builder()
              .key("S-03")
              .category(Category.STABILITY)
              .polarity(Polarity.POSITIVE)
              .text("규모/유동성이 비교적 안정적이라 우선 살펴볼 만해요.")
              .strength(Strength.WEAK)
              .build());
    }

    // M-01: 거래량 증가
    if (signals.getVolumeRatio() != null && signals.getVolumeRatio() >= VOLUME_RATIO_THRESHOLD) {
      triggeredRules.add("M-01");
      positiveCards.add(
          ReasonCard.builder()
              .key("M-01")
              .category(Category.MOMENTUM)
              .polarity(Polarity.POSITIVE)
              .text("거래량이 늘어 시장 관심이 붙는 구간이에요.")
              .strength(Strength.MEDIUM)
              .build());
    }

    // M-02: 5일 수익률 상위
    if (signals.getSectorReturn5dPercentile() != null
        && signals.getSectorReturn5dPercentile() <= TOP_RETURN_PERCENTILE) {
      triggeredRules.add("M-02");
      positiveCards.add(
          ReasonCard.builder()
              .key("M-02")
              .category(Category.MOMENTUM)
              .polarity(Polarity.POSITIVE)
              .text("단기 흐름이 섹터 내에서 상대적으로 강한 편이에요.")
              .strength(Strength.WEAK)
              .build());
    }
  }

  // ==================== Template Selection ====================

  /**
   * 템플릿 선택 우선순위: E(리스크) > B(모멘텀) > D(성장) > A(밸류) > C(안정) 동점이면 riskLevel↑, staleness↑, coverage↓일수록
   * 보수적으로(E/C)
   */
  private Template pickTemplate(
      List<String> triggeredRules, StockSignals signals, RuleFlags flags) {

    // E: 리스크형 - 하드필터/과열/부정뉴스
    if (flags.riskLevel == RiskLevel.HIGH || flags.hasOverheat || flags.hasNegativeNews) {
      return Template.E_RISK;
    }

    // B: 모멘텀형 - 거래량/수익률 강세
    boolean hasMomentum = triggeredRules.contains("M-01") || triggeredRules.contains("M-02");
    if (hasMomentum && !flags.hasLowCoverage) {
      return Template.B_MOMENTUM;
    }

    // D: 성장형 - 실적 개선
    if (triggeredRules.contains("P-01") || triggeredRules.contains("P-03")) {
      return Template.D_GROWTH;
    }

    // A: 밸류형 - PER/PBR 저평가
    if (triggeredRules.contains("V-01") || triggeredRules.contains("V-03")) {
      return Template.A_VALUE;
    }

    // C: 안정형 - 기본 또는 coverage 낮을 때
    return Template.C_STABLE;
  }

  // ==================== Summary Building ====================

  private InsightSummary buildSummary(
      Template template, StockSignals signals, RuleFlags flags, int score) {
    // P0-2: 톤 매칭 헤드라인 적용
    String headline = getToneMatchedHeadline(template, score, signals.getDataCoverage(), flags);
    Tone tone = (template == Template.E_RISK) ? Tone.CAUTIOUS_GUIDE : Tone.ACTIVE_GUIDE;
    ActionHint actionHint = getActionHintForTemplate(template);

    return InsightSummary.builder()
        .template(template)
        .headline(headline)
        .tone(tone)
        .actionHint(actionHint)
        .build();
  }

  /**
   * P0-2: 점수/coverage 기반 톤 매칭 헤드라인 생성
   *
   * <p>- score>=70 & coverage>=0.7 → 강한 톤 (우선 검토/유력 후보) - 50<=score<70 → 중간 톤 (관찰 리스트 상단) -
   * score<50 OR coverage<0.7 → 조건부 톤 (확인 후 접근) - HF-04 존재 시 조건부 프리픽스 자동 추가
   */
  private String getToneMatchedHeadline(
      Template template, int score, double coverage, RuleFlags flags) {

    // HF-04 (정보 부족) 시 조건부 프리픽스
    if (flags.hasLowCoverage) {
      return "정보가 제한적이지만, " + getConditionalHeadline(template);
    }

    // 강한 톤: score>=70 && coverage>=0.7
    if (score >= TONE_STRONG_SCORE && coverage >= TONE_STRONG_COVERAGE) {
      return getStrongToneHeadline(template);
    }

    // 중간 톤: 50<=score<70
    if (score >= TONE_MEDIUM_SCORE) {
      return getMediumToneHeadline(template);
    }

    // 조건부 톤: score<50 OR coverage<0.7
    return getConditionalHeadline(template);
  }

  /** 강한 톤 헤드라인 (score>=70 && coverage>=0.7) */
  private String getStrongToneHeadline(Template template) {
    return switch (template) {
      case A_VALUE -> "섹터 대비 부담이 낮아 우선 검토할 만한 유력 후보예요.";
      case B_MOMENTUM -> "추세가 강해서 단기 관찰 우선순위가 높은 종목이에요.";
      case C_STABLE -> "안정적인 흐름으로 초보자가 우선 검토하기 좋은 후보예요.";
      case D_GROWTH -> "실적 흐름이 좋아 유력 후보로 살펴볼 만해요.";
      case E_RISK -> "리스크 신호가 있어 관망이 기본이에요.";
    };
  }

  /** 중간 톤 헤드라인 (50<=score<70) */
  private String getMediumToneHeadline(Template template) {
    return switch (template) {
      case A_VALUE -> "밸류 측면은 괜찮아서 관찰 리스트 상단에 올려둘 만해요.";
      case B_MOMENTUM -> "단기 흐름이 있어 관찰 리스트에서 지켜볼 만해요.";
      case C_STABLE -> "무난한 안정형으로 관찰 리스트에 담아둘 만해요.";
      case D_GROWTH -> "성장 가능성이 있어 관찰하며 확인해 볼 종목이에요.";
      case E_RISK -> "리스크가 있어 관찰로만 접근하는 게 좋아요.";
    };
  }

  /** 조건부 톤 헤드라인 (score<50 OR coverage<0.7) */
  private String getConditionalHeadline(Template template) {
    return switch (template) {
      case A_VALUE -> "밸류 측면은 괜찮지만, 확인 후 접근이 좋아요.";
      case B_MOMENTUM -> "단기 흐름이 있으나 조건부로 관찰해 보세요.";
      case C_STABLE -> "안정적이나 추가 확인 후 접근이 좋아요.";
      case D_GROWTH -> "성장 기대가 있지만 실적 확인이 먼저예요.";
      case E_RISK -> "리스크가 있어 관망이 기본, 관찰로만 접근이 안전해요.";
    };
  }

  private String getHeadlineForTemplate(Template template) {
    return switch (template) {
      case A_VALUE -> "섹터 대비 부담이 낮아 우선 검토할 만한 후보예요.";
      case B_MOMENTUM -> "추세/관심이 강해서 단기 관찰 우선순위가 높은 종목이에요.";
      case C_STABLE -> "초보자 기준으로 무리 없이 들여다보기 좋은 안정형 후보예요.";
      case D_GROWTH -> "실적이 확인되면 강해질 유력 후보지만, 확인 후 접근이 좋아요.";
      case E_RISK -> "리스크 신호가 있어 관망이 기본, 관찰로만 접근이 안전해요.";
    };
  }

  private ActionHint getActionHintForTemplate(Template template) {
    return switch (template) {
      case A_VALUE ->
          ActionHint.builder()
              .text("실적 발표 일정과 섹터 내 경쟁사 비교를 확인해 보세요.")
              .focusKeys(List.of(FocusKey.SECTOR_COMPARISON, FocusKey.NEXT_EARNINGS))
              .build();
      case B_MOMENTUM ->
          ActionHint.builder()
              .text("거래량 추이와 뉴스를 모니터링하며 진입 타이밍을 관찰해 보세요.")
              .focusKeys(List.of(FocusKey.VOLUME_TREND, FocusKey.NEWS_RISK))
              .build();
      case C_STABLE ->
          ActionHint.builder()
              .text("실적 흐름과 배당 정책을 확인하며 천천히 살펴보세요.")
              .focusKeys(List.of(FocusKey.EARNINGS_TREND))
              .build();
      case D_GROWTH ->
          ActionHint.builder()
              .text("다음 실적 발표에서 성장 지속 여부를 확인해 보세요.")
              .focusKeys(List.of(FocusKey.EARNINGS_TREND, FocusKey.NEXT_EARNINGS))
              .build();
      case E_RISK ->
          ActionHint.builder()
              .text("리스크가 해소될 때까지 관찰 리스트에만 두는 것이 안전해요.")
              .focusKeys(List.of(FocusKey.NEWS_RISK, FocusKey.VOLATILITY))
              .build();
    };
  }

  // ==================== Card Selection ====================

  /** 긍정 카드 선택 - coverage 낮으면 2개, 아니면 3개 - 카테고리 다양성 유지 */
  private List<ReasonCard> selectPositiveCards(List<ReasonCard> cards, double coverage) {
    int maxCards =
        coverage < COVERAGE_LOW_THRESHOLD ? MAX_POSITIVE_CARDS_LOW_COVERAGE : MAX_POSITIVE_CARDS;

    // 강도 순으로 정렬
    List<ReasonCard> sorted =
        cards.stream()
            .sorted(Comparator.comparingInt(c -> -c.getStrength().ordinal()))
            .collect(Collectors.toList());

    // 카테고리 다양성 유지하며 선택
    List<ReasonCard> selected = new ArrayList<>();
    Set<Category> usedCategories = new HashSet<>();

    for (ReasonCard card : sorted) {
      if (selected.size() >= maxCards) break;
      if (!usedCategories.contains(card.getCategory()) || selected.size() < 2) {
        selected.add(card);
        usedCategories.add(card.getCategory());
      }
    }

    return selected;
  }

  /**
   * 주의 카드 선택 - 리스크/과열/부정뉴스가 있으면 2개, 아니면 1개 - 우선순위: RISK > MOMENTUM > NEWS > FUNDAMENTALS >
   * VALUATION
   */
  private List<ReasonCard> selectCautionCards(List<ReasonCard> cards, RuleFlags flags) {
    int maxCards =
        (flags.riskLevel == RiskLevel.HIGH || flags.hasOverheat || flags.hasNegativeNews)
            ? MAX_CAUTION_CARDS
            : MIN_CAUTION_CARDS;

    // 카테고리 우선순위로 정렬
    List<Category> priorityOrder =
        List.of(
            Category.RISK,
            Category.MOMENTUM,
            Category.NEWS,
            Category.FUNDAMENTALS,
            Category.VALUATION,
            Category.STABILITY);

    List<ReasonCard> sorted =
        cards.stream()
            .sorted(Comparator.comparingInt(c -> priorityOrder.indexOf(c.getCategory())))
            .collect(Collectors.toList());

    return sorted.stream().limit(maxCards).collect(Collectors.toList());
  }

  private ReasonCard buildDefaultCaution() {
    return ReasonCard.builder()
        .key("DEFAULT")
        .category(Category.RISK)
        .polarity(Polarity.CAUTION)
        .text("투자 판단 전 본인만의 기준을 꼭 확인하세요.")
        .strength(Strength.WEAK)
        .build();
  }

  // ==================== Score Building ====================

  private InsightScore buildScore(int adjustedScore, StockSignals signals, RuleFlags flags) {
    ScoreLabel grade = ScoreLabel.fromScore(adjustedScore);
    Confidence confidence =
        signals.getDataCoverage() >= 0.8
            ? Confidence.HIGH
            : (signals.getDataCoverage() >= 0.5 ? Confidence.MEDIUM : Confidence.LOW);

    return InsightScore.builder().value(adjustedScore).grade(grade).confidence(confidence).build();
  }

  private int calculateBaseScore(StockSignals signals) {
    // 기존 등락률 기반 점수 계산
    double changeRate = signals.getChangeRate();
    int score = (int) ((changeRate + 5) * 10);
    return Math.max(0, Math.min(100, score));
  }

  // ==================== Meta Building ====================

  private InsightMeta buildMeta(StockSignals signals) {
    return InsightMeta.builder()
        .asOf(LocalDateTime.now())
        .sources(List.of(Source.KRX))
        .coverage(signals.getDataCoverage())
        .stalenessSec(0)
        .build();
  }

  // ==================== News Building ====================

  private InsightNews buildNews(List<NewsItem> newsItems) {
    if (newsItems == null || newsItems.isEmpty()) {
      return InsightNews.builder().issueBrief(List.of()).headlineItems(List.of()).build();
    }

    // 중요도 + 신선도 순으로 정렬하고 상위 10개 선택
    List<NewsItem> sortedNews =
        newsItems.stream()
            .sorted(
                Comparator.comparingInt((NewsItem n) -> -n.getImportance().ordinal())
                    .thenComparing(NewsItem::getPublishedAt, Comparator.reverseOrder()))
            .limit(10)
            .collect(Collectors.toList());

    // 상위 3개로 issueBrief 생성
    List<String> issueBrief =
        sortedNews.stream().limit(3).map(NewsItem::getTitle).collect(Collectors.toList());

    return InsightNews.builder().issueBrief(issueBrief).headlineItems(sortedNews).build();
  }

  // ==================== Internal Classes ====================

  private enum RiskLevel {
    NORMAL,
    MEDIUM,
    HIGH
  }

  private static class RuleFlags {
    boolean topPicksEligible = true;
    RiskLevel riskLevel = RiskLevel.NORMAL;
    boolean hasValuationAnomaly = false;
    boolean hasLowCoverage = false;
    boolean hasOverheat = false;
    boolean hasNegativeNews = false;
  }
}
