package io.github.krails0105.stock_info_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.krails0105.stock_info_api.dto.insight.NewsItem;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Importance;
import io.github.krails0105.stock_info_api.dto.insight.NewsItem.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** NewsAggregatorService 테스트. */
class NewsAggregatorServiceTest {

  private NewsAggregatorService newsAggregatorService;

  @BeforeEach
  void setUp() {
    newsAggregatorService = new NewsAggregatorService();
  }

  @Nested
  @DisplayName("중요도 할당 테스트")
  class ImportanceTests {

    @Test
    @DisplayName("EARNINGS 태그는 HIGH 중요도")
    void testEarningsIsHigh() {
      NewsItem news =
          NewsItem.builder()
              .title("삼성전자 분기 실적 발표")
              .publisher("한경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/1")
              .tags(List.of(Tag.EARNINGS))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(news));

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getImportance()).isEqualTo(Importance.HIGH);
    }

    @Test
    @DisplayName("REGULATION_RISK 태그는 HIGH 중요도")
    void testRegulationRiskIsHigh() {
      NewsItem news =
          NewsItem.builder()
              .title("반도체 규제 리스크 부각")
              .publisher("매경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/2")
              .tags(List.of(Tag.REGULATION_RISK))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(news));

      assertThat(result.get(0).getImportance()).isEqualTo(Importance.HIGH);
    }

    @Test
    @DisplayName("RUMOR 태그는 LOW 중요도")
    void testRumorIsLow() {
      NewsItem news =
          NewsItem.builder()
              .title("삼성전자 인수설")
              .publisher("온라인")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/3")
              .tags(List.of(Tag.RUMOR))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(news));

      assertThat(result.get(0).getImportance()).isEqualTo(Importance.LOW);
    }
  }

  @Nested
  @DisplayName("중복 제거 테스트")
  class ClusteringTests {

    @Test
    @DisplayName("유사한 헤드라인은 하나로 클러스터링")
    void testSimilarHeadlinesClustered() {
      NewsItem news1 =
          NewsItem.builder()
              .title("삼성전자 분기 실적 호조")
              .publisher("한경")
              .publishedAt(LocalDateTime.now().minusHours(1))
              .url("https://example.com/1")
              .tags(List.of(Tag.EARNINGS))
              .build();

      NewsItem news2 =
          NewsItem.builder()
              .title("삼성전자 분기 실적 개선")
              .publisher("매경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/2")
              .tags(List.of(Tag.EARNINGS))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(news1, news2));

      // 유사한 헤드라인이므로 1개로 클러스터링
      assertThat(result).hasSize(1);
      // clusterId가 할당됨
      assertThat(result.get(0).getClusterId()).isNotNull();
    }

    @Test
    @DisplayName("다른 헤드라인은 각각 별도 유지")
    void testDifferentHeadlinesNotClustered() {
      NewsItem news1 =
          NewsItem.builder()
              .title("삼성전자 실적 발표")
              .publisher("한경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/1")
              .tags(List.of(Tag.EARNINGS))
              .build();

      NewsItem news2 =
          NewsItem.builder()
              .title("LG전자 신제품 출시")
              .publisher("매경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/2")
              .tags(List.of(Tag.CONTRACT))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(news1, news2));

      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("정렬 테스트")
  class SortingTests {

    @Test
    @DisplayName("중요도 높은 뉴스가 먼저")
    void testHighImportanceFirst() {
      NewsItem lowNews =
          NewsItem.builder()
              .title("시장 루머")
              .publisher("온라인")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/1")
              .tags(List.of(Tag.RUMOR))
              .build();

      NewsItem highNews =
          NewsItem.builder()
              .title("대규모 수주")
              .publisher("한경")
              .publishedAt(LocalDateTime.now().minusHours(1))
              .url("https://example.com/2")
              .tags(List.of(Tag.CONTRACT))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(lowNews, highNews));

      assertThat(result.get(0).getImportance()).isEqualTo(Importance.HIGH);
    }

    @Test
    @DisplayName("같은 중요도에서 최신 뉴스가 먼저")
    void testNewerNewsFirst() {
      NewsItem oldNews =
          NewsItem.builder()
              .title("실적 발표 A")
              .publisher("한경")
              .publishedAt(LocalDateTime.now().minusDays(2))
              .url("https://example.com/1")
              .tags(List.of(Tag.EARNINGS))
              .build();

      NewsItem newNews =
          NewsItem.builder()
              .title("실적 발표 B")
              .publisher("매경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/2")
              .tags(List.of(Tag.EARNINGS))
              .build();

      List<NewsItem> result = newsAggregatorService.aggregate(List.of(oldNews, newNews));

      // 최신 뉴스가 먼저
      assertThat(result.get(0).getPublishedAt()).isAfter(result.get(1).getPublishedAt());
    }
  }

  @Nested
  @DisplayName("RUMOR 필터링 테스트")
  class RumorFilterTests {

    @Test
    @DisplayName("RUMOR만 있는 뉴스 감지")
    void testIsRumorOnly() {
      NewsItem rumorOnly =
          NewsItem.builder()
              .title("인수설")
              .publisher("온라인")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/1")
              .tags(List.of(Tag.RUMOR))
              .build();

      NewsItem mixed =
          NewsItem.builder()
              .title("수주+루머")
              .publisher("한경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/2")
              .tags(List.of(Tag.CONTRACT, Tag.RUMOR))
              .build();

      assertThat(newsAggregatorService.isRumorOnly(rumorOnly)).isTrue();
      assertThat(newsAggregatorService.isRumorOnly(mixed)).isFalse();
    }

    @Test
    @DisplayName("근거 카드용 필터링에서 RUMOR only 제외")
    void testFilterForReasonCards() {
      NewsItem rumorOnly =
          NewsItem.builder()
              .title("인수설")
              .publisher("온라인")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/1")
              .tags(List.of(Tag.RUMOR))
              .build();

      NewsItem earnings =
          NewsItem.builder()
              .title("실적 발표")
              .publisher("한경")
              .publishedAt(LocalDateTime.now())
              .url("https://example.com/2")
              .tags(List.of(Tag.EARNINGS))
              .build();

      List<NewsItem> result =
          newsAggregatorService.filterForReasonCards(List.of(rumorOnly, earnings));

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTags()).contains(Tag.EARNINGS);
    }
  }
}
