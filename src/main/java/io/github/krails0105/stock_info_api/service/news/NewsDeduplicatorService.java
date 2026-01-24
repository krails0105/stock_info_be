package io.github.krails0105.stock_info_api.service.news;

import io.github.krails0105.stock_info_api.config.NewsProperties;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle;
import io.github.krails0105.stock_info_api.repository.ProcessedNewsArticleRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 뉴스 중복 제거 및 클러스터링 서비스.
 *
 * <p>Jaccard 유사도 기반으로 유사한 뉴스를 클러스터링하고 대표 기사를 선정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsDeduplicatorService {

  private final ProcessedNewsArticleRepository processedNewsRepository;
  private final NewsProperties newsProperties;

  /**
   * 새 기사에 클러스터 ID를 할당한다.
   *
   * <p>기존 클러스터 중 유사도가 임계값 이상인 것이 있으면 해당 클러스터에 포함, 없으면 새 클러스터 생성.
   *
   * @param title 기사 제목
   * @param publishedAt 발행 시각
   * @return 클러스터 할당 결과
   */
  public ClusterAssignment assignCluster(String title, LocalDateTime publishedAt) {
    double threshold = newsProperties.getClustering().getSimilarityThreshold();
    int windowHours = newsProperties.getClustering().getWindowHours();

    LocalDateTime windowStart = publishedAt.minusHours(windowHours);

    // 최근 클러스터 대표 기사들과 비교
    List<ProcessedNewsArticle> recentRepresentatives =
        processedNewsRepository.findRecentNews(
            windowStart, org.springframework.data.domain.PageRequest.of(0, 100));

    double maxSimilarity = 0.0;
    String matchedClusterId = null;

    for (ProcessedNewsArticle representative : recentRepresentatives) {
      if (!Boolean.TRUE.equals(representative.getIsClusterRepresentative())) {
        continue;
      }

      double similarity = calculateJaccardSimilarity(title, representative.getTitle());

      if (similarity > maxSimilarity) {
        maxSimilarity = similarity;
        if (similarity >= threshold) {
          matchedClusterId = representative.getClusterId();
        }
      }
    }

    if (matchedClusterId != null) {
      log.debug(
          "Article '{}' assigned to existing cluster {} (similarity={})",
          truncate(title, 30),
          matchedClusterId,
          maxSimilarity);
      return new ClusterAssignment(matchedClusterId, false, maxSimilarity);
    }

    String newClusterId = generateClusterId();
    log.debug(
        "Article '{}' assigned to new cluster {} (max similarity={})",
        truncate(title, 30),
        newClusterId,
        maxSimilarity);
    return new ClusterAssignment(newClusterId, true, maxSimilarity);
  }

  /**
   * Jaccard 유사도 계산.
   *
   * @param text1 첫 번째 텍스트
   * @param text2 두 번째 텍스트
   * @return 유사도 (0.0 ~ 1.0)
   */
  public double calculateJaccardSimilarity(String text1, String text2) {
    if (text1 == null || text2 == null) {
      return 0.0;
    }

    Set<String> tokens1 = tokenize(text1);
    Set<String> tokens2 = tokenize(text2);

    if (tokens1.isEmpty() || tokens2.isEmpty()) {
      return 0.0;
    }

    Set<String> intersection = new HashSet<>(tokens1);
    intersection.retainAll(tokens2);

    Set<String> union = new HashSet<>(tokens1);
    union.addAll(tokens2);

    return (double) intersection.size() / union.size();
  }

  /**
   * 텍스트 토큰화.
   *
   * @param text 원본 텍스트
   * @return 토큰 집합
   */
  private Set<String> tokenize(String text) {
    // 한글, 영문, 숫자만 추출 후 공백으로 분리
    String normalized =
        text.toLowerCase().replaceAll("[^가-힣a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim();

    if (normalized.isEmpty()) {
      return Set.of();
    }

    // 불용어 제거
    Set<String> stopwords =
        Set.of(
            "의", "를", "이", "가", "은", "는", "에", "도", "와", "과", "로", "a", "the", "is", "are", "and",
            "or");

    Set<String> tokens = new HashSet<>(Arrays.asList(normalized.split("\\s+")));
    tokens.removeAll(stopwords);

    // 1글자 토큰 제거
    tokens.removeIf(t -> t.length() < 2);

    return tokens;
  }

  private String generateClusterId() {
    return "cluster-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private String truncate(String text, int maxLength) {
    if (text == null) {
      return "";
    }
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
  }

  /** 클러스터 할당 결과. */
  public record ClusterAssignment(String clusterId, boolean isNewCluster, double similarityScore) {}
}
