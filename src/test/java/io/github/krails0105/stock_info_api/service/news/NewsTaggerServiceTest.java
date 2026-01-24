package io.github.krails0105.stock_info_api.service.news;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsImportance;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsTag;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** NewsTaggerService 테스트. */
class NewsTaggerServiceTest {

  private NewsTaggerService taggerService;

  @BeforeEach
  void setUp() {
    taggerService = new NewsTaggerService();
  }

  @Nested
  @DisplayName("태그 할당 테스트")
  class TagAssignmentTests {

    @Test
    @DisplayName("실적 관련 키워드 → EARNINGS 태그")
    void testEarningsTag() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("삼성전자 2분기 영업이익 사상 최대")
              .content("영업이익이 전년 대비 50% 증가")
              .publishedAt(LocalDateTime.now())
              .build();

      List<NewsTag> tags = taggerService.assignTags(article);

      assertThat(tags).contains(NewsTag.EARNINGS);
    }

    @Test
    @DisplayName("계약/수주 키워드 → CONTRACT 태그")
    void testContractTag() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("현대차 미국 대규모 수주 계약 체결")
              .content("10조원 규모 납품 계약")
              .publishedAt(LocalDateTime.now())
              .build();

      List<NewsTag> tags = taggerService.assignTags(article);

      assertThat(tags).contains(NewsTag.CONTRACT);
    }

    @Test
    @DisplayName("배당/자사주 키워드 → BUYBACK_DIVIDEND 태그")
    void testBuybackDividendTag() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("SK하이닉스 자사주 매입 결정")
              .content("주주환원 강화")
              .publishedAt(LocalDateTime.now())
              .build();

      List<NewsTag> tags = taggerService.assignTags(article);

      assertThat(tags).contains(NewsTag.BUYBACK_DIVIDEND);
    }

    @Test
    @DisplayName("규제 리스크 키워드 → REGULATION_RISK 태그")
    void testRegulationRiskTag() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("반도체 수출 규제 우려 고조")
              .content("검찰 수사 진행")
              .publishedAt(LocalDateTime.now())
              .build();

      List<NewsTag> tags = taggerService.assignTags(article);

      assertThat(tags).contains(NewsTag.REGULATION_RISK);
    }

    @Test
    @DisplayName("M&A 키워드 → MA 태그")
    void testMaTag() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("네이버 해외 기업 인수 추진")
              .content("M&A 통한 사업 확장")
              .publishedAt(LocalDateTime.now())
              .build();

      List<NewsTag> tags = taggerService.assignTags(article);

      assertThat(tags).contains(NewsTag.MA);
    }

    @Test
    @DisplayName("매칭 키워드 없으면 INDUSTRY 기본 태그")
    void testDefaultIndustryTag() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("올해 주식시장 전망")
              .content("경기 침체 우려")
              .publishedAt(LocalDateTime.now())
              .build();

      List<NewsTag> tags = taggerService.assignTags(article);

      assertThat(tags).contains(NewsTag.INDUSTRY);
    }
  }

  @Nested
  @DisplayName("중요도 결정 테스트")
  class ImportanceTests {

    @Test
    @DisplayName("24시간 이내 EARNINGS 뉴스 → HIGH")
    void testHighImportance() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("삼성전자 실적 발표")
              .publisher("연합뉴스")
              .publishedAt(LocalDateTime.now().minusHours(12))
              .build();

      List<NewsTag> tags = List.of(NewsTag.EARNINGS);
      NewsImportance importance = taggerService.determineImportance(article, tags);

      assertThat(importance).isEqualTo(NewsImportance.HIGH);
    }

    @Test
    @DisplayName("오래된 뉴스 → 중요도 낮음")
    void testLowImportanceForOldNews() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("일반 시장 뉴스")
              .publisher("알 수 없음")
              .publishedAt(LocalDateTime.now().minusDays(5))
              .build();

      List<NewsTag> tags = List.of(NewsTag.INDUSTRY);
      NewsImportance importance = taggerService.determineImportance(article, tags);

      assertThat(importance).isEqualTo(NewsImportance.LOW);
    }
  }

  @Nested
  @DisplayName("종목 코드 추출 테스트")
  class StockCodeExtractionTests {

    @Test
    @DisplayName("6자리 숫자 패턴 추출")
    void testExtractStockCode() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("삼성전자(005930) 실적 발표")
              .content("코스피 대장주")
              .publishedAt(LocalDateTime.now())
              .build();

      String stockCode = taggerService.extractStockCode(article);

      assertThat(stockCode).isEqualTo("005930");
    }

    @Test
    @DisplayName("종목 코드 없으면 null")
    void testNoStockCode() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("일반 시장 뉴스")
              .content("전체 시장 동향")
              .publishedAt(LocalDateTime.now())
              .build();

      String stockCode = taggerService.extractStockCode(article);

      assertThat(stockCode).isNull();
    }
  }

  @Nested
  @DisplayName("섹터 추출 테스트")
  class SectorExtractionTests {

    @Test
    @DisplayName("반도체 키워드 → 반도체 섹터")
    void testSemiconductorSector() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("반도체 업황 개선")
              .content("메모리 가격 상승")
              .publishedAt(LocalDateTime.now())
              .build();

      String sector = taggerService.extractSectorName(article);

      assertThat(sector).isEqualTo("반도체");
    }

    @Test
    @DisplayName("자동차 키워드 → 자동차 섹터")
    void testAutoSector() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("전기차 판매량 급증")
              .content("EV 시장 성장")
              .publishedAt(LocalDateTime.now())
              .build();

      String sector = taggerService.extractSectorName(article);

      assertThat(sector).isEqualTo("자동차");
    }

    @Test
    @DisplayName("섹터 매칭 없으면 null")
    void testNoSector() {
      RawNewsArticle article =
          RawNewsArticle.builder()
              .title("일반 경제 뉴스")
              .content("GDP 성장률 발표")
              .publishedAt(LocalDateTime.now())
              .build();

      String sector = taggerService.extractSectorName(article);

      assertThat(sector).isNull();
    }
  }
}
