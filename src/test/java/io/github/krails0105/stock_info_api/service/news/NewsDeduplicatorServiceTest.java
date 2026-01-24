package io.github.krails0105.stock_info_api.service.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.krails0105.stock_info_api.config.NewsProperties;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle;
import io.github.krails0105.stock_info_api.repository.ProcessedNewsArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** NewsDeduplicatorService 테스트. */
class NewsDeduplicatorServiceTest {

  private NewsDeduplicatorService deduplicatorService;
  private ProcessedNewsArticleRepository processedNewsRepository;
  private NewsProperties newsProperties;

  @BeforeEach
  void setUp() {
    processedNewsRepository = mock(ProcessedNewsArticleRepository.class);
    newsProperties = new NewsProperties();
    newsProperties.getClustering().setSimilarityThreshold(0.6);
    newsProperties.getClustering().setWindowHours(72);

    deduplicatorService = new NewsDeduplicatorService(processedNewsRepository, newsProperties);
  }

  @Nested
  @DisplayName("Jaccard 유사도 계산 테스트")
  class SimilarityTests {

    @Test
    @DisplayName("동일한 텍스트 → 유사도 1.0")
    void testIdenticalTexts() {
      double similarity =
          deduplicatorService.calculateJaccardSimilarity("삼성전자 실적 발표", "삼성전자 실적 발표");

      assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("유사한 텍스트 → 높은 유사도")
    void testSimilarTexts() {
      double similarity =
          deduplicatorService.calculateJaccardSimilarity("삼성전자 분기 실적 호조", "삼성전자 분기 실적 개선");

      // 삼성전자, 분기, 실적 공통 → 높은 유사도
      assertThat(similarity).isGreaterThan(0.5);
    }

    @Test
    @DisplayName("다른 텍스트 → 낮은 유사도")
    void testDifferentTexts() {
      double similarity = deduplicatorService.calculateJaccardSimilarity("삼성전자 실적 발표", "현대차 신차 출시");

      assertThat(similarity).isLessThan(0.3);
    }

    @Test
    @DisplayName("null 입력 → 유사도 0")
    void testNullInput() {
      double similarity = deduplicatorService.calculateJaccardSimilarity(null, "테스트");

      assertThat(similarity).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("클러스터 할당 테스트")
  class ClusterAssignmentTests {

    @Test
    @DisplayName("유사한 기사 없으면 새 클러스터 생성")
    void testNewClusterCreation() {
      when(processedNewsRepository.findRecentNews(any(), any(Pageable.class)))
          .thenReturn(List.of());

      NewsDeduplicatorService.ClusterAssignment assignment =
          deduplicatorService.assignCluster("새로운 뉴스 제목", LocalDateTime.now());

      assertThat(assignment.isNewCluster()).isTrue();
      assertThat(assignment.clusterId()).startsWith("cluster-");
    }

    @Test
    @DisplayName("유사한 기사 있으면 기존 클러스터에 할당")
    void testExistingClusterAssignment() {
      ProcessedNewsArticle existingArticle =
          ProcessedNewsArticle.builder()
              .title("삼성전자 분기 실적 발표")
              .clusterId("cluster-existing")
              .isClusterRepresentative(true)
              .build();

      when(processedNewsRepository.findRecentNews(any(), any(Pageable.class)))
          .thenReturn(List.of(existingArticle));

      NewsDeduplicatorService.ClusterAssignment assignment =
          deduplicatorService.assignCluster("삼성전자 분기 실적 호조", LocalDateTime.now());

      assertThat(assignment.isNewCluster()).isFalse();
      assertThat(assignment.clusterId()).isEqualTo("cluster-existing");
    }

    @Test
    @DisplayName("유사도가 임계값 미만이면 새 클러스터")
    void testBelowThresholdNewCluster() {
      ProcessedNewsArticle existingArticle =
          ProcessedNewsArticle.builder()
              .title("현대차 신차 출시")
              .clusterId("cluster-hyundai")
              .isClusterRepresentative(true)
              .build();

      when(processedNewsRepository.findRecentNews(any(), any(Pageable.class)))
          .thenReturn(List.of(existingArticle));

      NewsDeduplicatorService.ClusterAssignment assignment =
          deduplicatorService.assignCluster("삼성전자 실적 발표", LocalDateTime.now());

      // 다른 내용이므로 새 클러스터
      assertThat(assignment.isNewCluster()).isTrue();
    }
  }
}
