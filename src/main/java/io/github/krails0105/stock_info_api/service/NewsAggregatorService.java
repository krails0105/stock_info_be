package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.config.NewsProperties;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Importance;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Tag;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle;
import io.github.krails0105.stock_info_api.repository.ProcessedNewsArticleRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 뉴스 집계 서비스.
 *
 * <p>스펙 요구사항:
 *
 * <ul>
 *   <li>태그 기반 중요도 분류 (HIGH: EARNINGS/CONTRACT/BUYBACK_DIVIDEND/REGULATION_RISK)
 *   <li>중복 제거 (유사 헤드라인 클러스터링, 대표 1개만 상단 노출)
 *   <li>신선도 우선 정렬 (24~72시간 우선)
 *   <li>RUMOR만 있는 경우 근거 카드 반영 금지 (리스트 표시만)
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAggregatorService {

  private final ProcessedNewsArticleRepository processedNewsRepository;
  private final NewsProperties newsProperties;

  private static final int FRESHNESS_HOURS_HIGH = 24;
  private static final int FRESHNESS_HOURS_MEDIUM = 72;
  private static final double SIMILARITY_THRESHOLD = 0.6;

  /**
   * 뉴스 목록을 집계하여 중복 제거, 중요도/신선도 정렬 수행.
   *
   * @param newsItems 원본 뉴스 목록
   * @return 집계된 뉴스 목록 (중복 제거, 정렬 완료)
   */
  public List<NewsItem> aggregate(List<NewsItem> newsItems) {
    if (newsItems == null || newsItems.isEmpty()) {
      return List.of();
    }

    // 1. 중요도 태깅
    List<NewsItem> taggedNews = assignImportance(newsItems);

    // 2. 클러스터링 (중복 제거)
    List<NewsItem> clusteredNews = clusterByHeadline(taggedNews);

    // 3. 중요도 + 신선도 정렬
    return sortByImportanceAndFreshness(clusteredNews);
  }

  /**
   * 태그 기반으로 중요도 할당. HIGH: EARNINGS, CONTRACT, BUYBACK_DIVIDEND, REGULATION_RISK MEDIUM: MA,
   * INDUSTRY LOW: RUMOR, 기타
   */
  private List<NewsItem> assignImportance(List<NewsItem> newsItems) {
    return newsItems.stream().map(this::assignImportanceToItem).collect(Collectors.toList());
  }

  private NewsItem assignImportanceToItem(NewsItem item) {
    if (item.getImportance() != null) {
      return item; // 이미 할당됨
    }

    Importance importance = calculateImportance(item.getTags());

    return NewsItem.builder()
        .title(item.getTitle())
        .publisher(item.getPublisher())
        .publishedAt(item.getPublishedAt())
        .url(item.getUrl())
        .tags(item.getTags())
        .importance(importance)
        .clusterId(item.getClusterId())
        .build();
  }

  private Importance calculateImportance(List<Tag> tags) {
    if (tags == null || tags.isEmpty()) {
      return Importance.LOW;
    }

    // HIGH 우선순위 태그
    if (tags.contains(Tag.EARNINGS)
        || tags.contains(Tag.CONTRACT)
        || tags.contains(Tag.BUYBACK_DIVIDEND)
        || tags.contains(Tag.REGULATION_RISK)) {
      return Importance.HIGH;
    }

    // MEDIUM 태그
    if (tags.contains(Tag.MA) || tags.contains(Tag.INDUSTRY)) {
      return Importance.MEDIUM;
    }

    // RUMOR 또는 기타
    return Importance.LOW;
  }

  /** 유사 헤드라인 클러스터링. 유사도가 SIMILARITY_THRESHOLD 이상인 뉴스는 같은 클러스터로 묶고, 각 클러스터에서 가장 중요하고 최신인 뉴스만 반환. */
  private List<NewsItem> clusterByHeadline(List<NewsItem> newsItems) {
    Map<String, List<NewsItem>> clusters = new HashMap<>();
    List<NewsItem> result = new ArrayList<>();

    for (NewsItem item : newsItems) {
      String clusterId = findMatchingCluster(item, clusters);

      if (clusterId == null) {
        // 새 클러스터 생성
        clusterId = UUID.randomUUID().toString().substring(0, 8);
        clusters.put(clusterId, new ArrayList<>());
      }

      NewsItem itemWithCluster =
          NewsItem.builder()
              .title(item.getTitle())
              .publisher(item.getPublisher())
              .publishedAt(item.getPublishedAt())
              .url(item.getUrl())
              .tags(item.getTags())
              .importance(item.getImportance())
              .clusterId(clusterId)
              .build();

      clusters.get(clusterId).add(itemWithCluster);
    }

    // 각 클러스터에서 대표 뉴스 선택 (가장 중요하고 최신)
    for (List<NewsItem> cluster : clusters.values()) {
      NewsItem representative = selectRepresentative(cluster);
      result.add(representative);
    }

    return result;
  }

  /** 기존 클러스터 중 유사한 헤드라인이 있는지 검색. */
  private String findMatchingCluster(NewsItem item, Map<String, List<NewsItem>> clusters) {
    for (Map.Entry<String, List<NewsItem>> entry : clusters.entrySet()) {
      for (NewsItem existing : entry.getValue()) {
        if (calculateSimilarity(item.getTitle(), existing.getTitle()) >= SIMILARITY_THRESHOLD) {
          return entry.getKey();
        }
      }
    }
    return null;
  }

  /** 간단한 자카드 유사도 계산 (단어 기반). */
  private double calculateSimilarity(String title1, String title2) {
    if (title1 == null || title2 == null) {
      return 0.0;
    }

    String[] words1 = title1.toLowerCase().split("\\s+");
    String[] words2 = title2.toLowerCase().split("\\s+");

    java.util.Set<String> set1 = new java.util.HashSet<>(java.util.Arrays.asList(words1));
    java.util.Set<String> set2 = new java.util.HashSet<>(java.util.Arrays.asList(words2));

    java.util.Set<String> intersection = new java.util.HashSet<>(set1);
    intersection.retainAll(set2);

    java.util.Set<String> union = new java.util.HashSet<>(set1);
    union.addAll(set2);

    if (union.isEmpty()) {
      return 0.0;
    }

    return (double) intersection.size() / union.size();
  }

  /** 클러스터에서 대표 뉴스 선택 (중요도 높고 최신). */
  private NewsItem selectRepresentative(List<NewsItem> cluster) {
    return cluster.stream()
        .max(
            Comparator.comparingInt((NewsItem n) -> n.getImportance().ordinal())
                .thenComparing(NewsItem::getPublishedAt))
        .orElse(cluster.get(0));
  }

  /**
   * 중요도 + 신선도 기반 정렬. 1차: 중요도 (HIGH > MEDIUM > LOW) 2차: 신선도 (24시간 내 > 72시간 내 > 이후) 3차: 발행일 (최신 우선)
   */
  private List<NewsItem> sortByImportanceAndFreshness(List<NewsItem> newsItems) {
    LocalDateTime now = LocalDateTime.now();

    return newsItems.stream()
        .sorted(
            Comparator
                // 1차: 중요도 (HIGH=0, MEDIUM=1, LOW=2 → 오름차순이 HIGH 우선)
                .comparingInt((NewsItem n) -> n.getImportance().ordinal())
                // 2차: 신선도 점수 (높을수록 우선 → 역순)
                .thenComparingInt(n -> -getFreshnessScore(n.getPublishedAt(), now))
                // 3차: 발행일 (최신 우선)
                .thenComparing(NewsItem::getPublishedAt, Comparator.reverseOrder()))
        .collect(Collectors.toList());
  }

  /** 신선도 점수 계산. 24시간 내: 3점 72시간 내: 2점 그 외: 1점 */
  private int getFreshnessScore(LocalDateTime publishedAt, LocalDateTime now) {
    if (publishedAt == null) {
      return 0;
    }

    long hoursAgo = ChronoUnit.HOURS.between(publishedAt, now);

    if (hoursAgo <= FRESHNESS_HOURS_HIGH) {
      return 3;
    } else if (hoursAgo <= FRESHNESS_HOURS_MEDIUM) {
      return 2;
    } else {
      return 1;
    }
  }

  /** RUMOR만 있는 뉴스인지 확인. 근거 카드에는 반영하지 않고 리스트 표시만 허용. */
  public boolean isRumorOnly(NewsItem item) {
    if (item.getTags() == null || item.getTags().isEmpty()) {
      return false;
    }
    return item.getTags().size() == 1 && item.getTags().contains(Tag.RUMOR);
  }

  /** 근거 카드에 사용 가능한 뉴스만 필터링 (RUMOR only 제외). */
  public List<NewsItem> filterForReasonCards(List<NewsItem> newsItems) {
    return newsItems.stream().filter(item -> !isRumorOnly(item)).collect(Collectors.toList());
  }

  /**
   * DB에서 종목 관련 뉴스 조회 후 집계.
   *
   * @param stockCode 종목 코드
   * @return 집계된 뉴스 목록
   */
  public List<NewsItem> getNewsByStockCode(String stockCode) {
    int windowHours = newsProperties.getClustering().getWindowHours();
    LocalDateTime since = LocalDateTime.now().minusHours(windowHours);

    List<ProcessedNewsArticle> articles =
        processedNewsRepository.findRepresentativeNewsByStockCode(stockCode, since);

    List<NewsItem> newsItems = articles.stream().map(this::toNewsItem).collect(Collectors.toList());

    return sortByImportanceAndFreshness(newsItems);
  }

  /**
   * DB에서 섹터 관련 뉴스 조회 후 집계.
   *
   * @param sectorName 섹터명
   * @return 집계된 뉴스 목록
   */
  public List<NewsItem> getNewsBySectorName(String sectorName) {
    int windowHours = newsProperties.getClustering().getWindowHours();
    LocalDateTime since = LocalDateTime.now().minusHours(windowHours);

    List<ProcessedNewsArticle> articles =
        processedNewsRepository.findRepresentativeNewsBySectorName(sectorName, since);

    List<NewsItem> newsItems = articles.stream().map(this::toNewsItem).collect(Collectors.toList());

    return sortByImportanceAndFreshness(newsItems);
  }

  /**
   * ProcessedNewsArticle → NewsItem 변환.
   *
   * @param article DB 엔티티
   * @return DTO
   */
  private NewsItem toNewsItem(ProcessedNewsArticle article) {
    List<Tag> tags =
        article.getTags() != null
            ? article.getTags().stream()
                .map(t -> Tag.valueOf(t.name()))
                .collect(Collectors.toList())
            : List.of();

    Importance importance =
        article.getImportance() != null
            ? Importance.valueOf(article.getImportance().name())
            : Importance.LOW;

    return NewsItem.builder()
        .title(article.getTitle())
        .publisher(article.getPublisher())
        .publishedAt(article.getPublishedAt())
        .url(article.getUrl())
        .tags(tags)
        .importance(importance)
        .clusterId(article.getClusterId())
        .build();
  }
}
