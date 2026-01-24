package io.github.krails0105.stock_info_api.service.news;

import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsImportance;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsTag;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 뉴스 태깅 서비스.
 *
 * <p>키워드 기반 규칙으로 뉴스에 태그와 중요도를 부여한다. 향후 LLM 기반 태깅으로 확장 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsTaggerService {

  /** 태그별 키워드 패턴. */
  private static final Map<NewsTag, List<Pattern>> TAG_PATTERNS =
      Map.of(
          NewsTag.EARNINGS,
          List.of(
              Pattern.compile("실적", Pattern.CASE_INSENSITIVE),
              Pattern.compile("분기", Pattern.CASE_INSENSITIVE),
              Pattern.compile("영업이익", Pattern.CASE_INSENSITIVE),
              Pattern.compile("순이익", Pattern.CASE_INSENSITIVE),
              Pattern.compile("매출", Pattern.CASE_INSENSITIVE),
              Pattern.compile("어닝[스]?", Pattern.CASE_INSENSITIVE),
              Pattern.compile("earnings", Pattern.CASE_INSENSITIVE)),
          NewsTag.CONTRACT,
          List.of(
              Pattern.compile("계약", Pattern.CASE_INSENSITIVE),
              Pattern.compile("수주", Pattern.CASE_INSENSITIVE),
              Pattern.compile("납품", Pattern.CASE_INSENSITIVE),
              Pattern.compile("공급", Pattern.CASE_INSENSITIVE)),
          NewsTag.BUYBACK_DIVIDEND,
          List.of(
              Pattern.compile("배당", Pattern.CASE_INSENSITIVE),
              Pattern.compile("자사주", Pattern.CASE_INSENSITIVE),
              Pattern.compile("매입", Pattern.CASE_INSENSITIVE),
              Pattern.compile("주주환원", Pattern.CASE_INSENSITIVE)),
          NewsTag.REGULATION_RISK,
          List.of(
              Pattern.compile("규제", Pattern.CASE_INSENSITIVE),
              Pattern.compile("제재", Pattern.CASE_INSENSITIVE),
              Pattern.compile("과징금", Pattern.CASE_INSENSITIVE),
              Pattern.compile("수사", Pattern.CASE_INSENSITIVE),
              Pattern.compile("검찰", Pattern.CASE_INSENSITIVE),
              Pattern.compile("소송", Pattern.CASE_INSENSITIVE)),
          NewsTag.MA,
          List.of(
              Pattern.compile("인수", Pattern.CASE_INSENSITIVE),
              Pattern.compile("합병", Pattern.CASE_INSENSITIVE),
              Pattern.compile("M&A", Pattern.CASE_INSENSITIVE),
              Pattern.compile("M\\s*&\\s*A", Pattern.CASE_INSENSITIVE)),
          NewsTag.INDUSTRY,
          List.of(
              Pattern.compile("업종", Pattern.CASE_INSENSITIVE),
              Pattern.compile("산업", Pattern.CASE_INSENSITIVE),
              Pattern.compile("섹터", Pattern.CASE_INSENSITIVE),
              Pattern.compile("시장", Pattern.CASE_INSENSITIVE)),
          NewsTag.RUMOR,
          List.of(
              Pattern.compile("루머", Pattern.CASE_INSENSITIVE),
              Pattern.compile("소문", Pattern.CASE_INSENSITIVE),
              Pattern.compile("찌라시", Pattern.CASE_INSENSITIVE),
              Pattern.compile("관측", Pattern.CASE_INSENSITIVE)));

  /** 신뢰할 수 있는 언론사 목록 (신선도 가산점용). */
  private static final List<String> TRUSTED_PUBLISHERS =
      List.of("연합뉴스", "한국경제", "매일경제", "조선비즈", "서울경제", "머니투데이", "뉴스1");

  /**
   * 뉴스에 태그를 부여한다.
   *
   * @param article 원본 기사
   * @return 부여된 태그 목록
   */
  public List<NewsTag> assignTags(RawNewsArticle article) {
    List<NewsTag> tags = new ArrayList<>();
    String text = buildSearchText(article);

    for (Map.Entry<NewsTag, List<Pattern>> entry : TAG_PATTERNS.entrySet()) {
      for (Pattern pattern : entry.getValue()) {
        if (pattern.matcher(text).find()) {
          tags.add(entry.getKey());
          break;
        }
      }
    }

    if (tags.isEmpty()) {
      tags.add(NewsTag.INDUSTRY);
    }

    log.debug("Assigned tags {} to article: {}", tags, article.getTitle());
    return tags;
  }

  /**
   * 뉴스 중요도를 결정한다.
   *
   * @param article 원본 기사
   * @param tags 부여된 태그
   * @return 중요도
   */
  public NewsImportance determineImportance(RawNewsArticle article, List<NewsTag> tags) {
    int score = 0;

    // 신선도 점수 (24시간 이내: +3, 72시간 이내: +1)
    if (article.getPublishedAt() != null) {
      long hoursAgo = ChronoUnit.HOURS.between(article.getPublishedAt(), LocalDateTime.now());
      if (hoursAgo <= 24) {
        score += 3;
      } else if (hoursAgo <= 72) {
        score += 1;
      }
    }

    // 태그별 점수
    if (tags.contains(NewsTag.EARNINGS)) {
      score += 3;
    }
    if (tags.contains(NewsTag.CONTRACT)) {
      score += 2;
    }
    if (tags.contains(NewsTag.MA)) {
      score += 3;
    }
    if (tags.contains(NewsTag.REGULATION_RISK)) {
      score += 2;
    }
    if (tags.contains(NewsTag.BUYBACK_DIVIDEND)) {
      score += 2;
    }

    // 신뢰 언론사 가산점
    if (isTrustedPublisher(article.getPublisher())) {
      score += 1;
    }

    NewsImportance importance;
    if (score >= 5) {
      importance = NewsImportance.HIGH;
    } else if (score >= 2) {
      importance = NewsImportance.MEDIUM;
    } else {
      importance = NewsImportance.LOW;
    }

    log.debug(
        "Determined importance {} (score={}) for article: {}",
        importance,
        score,
        article.getTitle());
    return importance;
  }

  /**
   * 종목 코드 추출 (간단한 패턴 매칭).
   *
   * @param article 원본 기사
   * @return 종목 코드 (없으면 null)
   */
  public String extractStockCode(RawNewsArticle article) {
    // 간단한 구현: 6자리 숫자 패턴 찾기 (한국 주식 코드)
    String text = buildSearchText(article);
    Pattern stockCodePattern = Pattern.compile("\\b(\\d{6})\\b");
    java.util.regex.Matcher matcher = stockCodePattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * 섹터명 추출 (키워드 기반).
   *
   * @param article 원본 기사
   * @return 섹터명 (없으면 null)
   */
  public String extractSectorName(RawNewsArticle article) {
    String text = buildSearchText(article);

    Map<String, List<String>> sectorKeywords =
        Map.of(
            "반도체",
            List.of("반도체", "메모리", "파운드리", "HBM"),
            "자동차",
            List.of("자동차", "전기차", "EV", "배터리"),
            "바이오",
            List.of("바이오", "제약", "신약", "임상"),
            "금융",
            List.of("은행", "증권", "보험", "금융"),
            "IT/소프트웨어",
            List.of("AI", "인공지능", "소프트웨어", "클라우드"));

    for (Map.Entry<String, List<String>> entry : sectorKeywords.entrySet()) {
      for (String keyword : entry.getValue()) {
        if (text.contains(keyword)) {
          return entry.getKey();
        }
      }
    }

    return null;
  }

  private String buildSearchText(RawNewsArticle article) {
    StringBuilder sb = new StringBuilder();
    if (article.getTitle() != null) {
      sb.append(article.getTitle()).append(" ");
    }
    if (article.getContent() != null) {
      sb.append(article.getContent());
    }
    return sb.toString();
  }

  private boolean isTrustedPublisher(String publisher) {
    if (publisher == null) {
      return false;
    }
    return TRUSTED_PUBLISHERS.stream().anyMatch(publisher::contains);
  }
}
