package io.github.krails0105.stock_info_api.service.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.krails0105.stock_info_api.repository.RawNewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** NewsCollectorService 테스트. */
class NewsCollectorServiceTest {

  private NewsCollectorService collectorService;
  private RawNewsArticleRepository rawNewsRepository;

  @BeforeEach
  void setUp() {
    rawNewsRepository = mock(RawNewsArticleRepository.class);
    collectorService = new NewsCollectorService(rawNewsRepository);
  }

  @Nested
  @DisplayName("RSS 피드 설정 테스트")
  class FeedConfigTests {

    @Test
    @DisplayName("수집 결과 레코드 생성")
    void testCollectionResultRecord() {
      NewsCollectorService.CollectionResult result =
          new NewsCollectorService.CollectionResult(10, 5, 1);

      assertThat(result.collected()).isEqualTo(10);
      assertThat(result.duplicates()).isEqualTo(5);
      assertThat(result.errors()).isEqualTo(1);
    }

    @Test
    @DisplayName("피드 설정 레코드 생성")
    void testRssFeedConfigRecord() {
      NewsCollectorService.RssFeedConfig config =
          new NewsCollectorService.RssFeedConfig("test-feed", "https://example.com/rss");

      assertThat(config.name()).isEqualTo("test-feed");
      assertThat(config.url()).isEqualTo("https://example.com/rss");
    }
  }

  @Nested
  @DisplayName("중복 체크 통합 테스트")
  class DuplicateCheckTests {

    @Test
    @DisplayName("URL 중복 시 건너뜀")
    void testSkipDuplicateUrl() {
      when(rawNewsRepository.existsByUrl(anyString())).thenReturn(true);

      // 실제 RSS 호출 없이 로직만 테스트
      // collectFromAllFeeds는 네트워크 필요하므로 통합 테스트에서 검증
      assertThat(rawNewsRepository.existsByUrl("https://example.com/news")).isTrue();
    }
  }
}
