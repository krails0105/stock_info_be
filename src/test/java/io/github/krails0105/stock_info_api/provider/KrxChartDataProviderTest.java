package io.github.krails0105.stock_info_api.provider;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.krails0105.stock_info_api.dto.external.naver.NaverChartResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * KrxChartDataProvider 단위 테스트.
 *
 * <p>네이버 차트 API XML 파싱 로직을 검증한다.
 */
class KrxChartDataProviderTest {

  @Nested
  @DisplayName("NaverChartResponse XML 파싱 테스트")
  class NaverChartResponseParsingTest {

    @Test
    @DisplayName("일봉 데이터 XML 파싱 성공")
    void shouldParseDailyChartXml() {
      // given
      String xml =
          """
          <?xml version="1.0" encoding="euc-kr" ?>
          <protocol>
          <chartdata symbol="005930">
          <item data="20260127|72000|72500|71500|72200|15000000" />
          <item data="20260128|72200|73000|72000|72800|18000000" />
          <item data="20260129|72800|73500|72500|73200|20000000" />
          </chartdata>
          </protocol>
          """;

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).hasSize(3);

      var firstItem = response.getItems().get(0);
      assertThat(firstItem.getDate()).isEqualTo("20260127");
      assertThat(firstItem.getOpenPrice()).isEqualTo(72000L);
      assertThat(firstItem.getHighPrice()).isEqualTo(72500L);
      assertThat(firstItem.getLowPrice()).isEqualTo(71500L);
      assertThat(firstItem.getClosePrice()).isEqualTo(72200L);
      assertThat(firstItem.getVolume()).isEqualTo(15000000L);
    }

    @Test
    @DisplayName("분봉 데이터 XML 파싱 성공")
    void shouldParseMinuteChartXml() {
      // given
      String xml =
          """
          <?xml version="1.0" encoding="euc-kr" ?>
          <protocol>
          <chartdata symbol="005930">
          <item data="202601301000|72000|72100|71900|72050|500000" />
          <item data="202601301005|72050|72200|72000|72150|600000" />
          </chartdata>
          </protocol>
          """;

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).hasSize(2);

      var firstItem = response.getItems().get(0);
      assertThat(firstItem.getDate()).isEqualTo("202601301000");
      assertThat(firstItem.getClosePrice()).isEqualTo(72050L);
      assertThat(firstItem.getVolume()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("빈 XML 파싱 시 빈 리스트 반환")
    void shouldReturnEmptyListForEmptyXml() {
      // given
      String xml = "<?xml version=\"1.0\" encoding=\"euc-kr\" ?><protocol></protocol>";

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("잘못된 데이터 형식은 스킵")
    void shouldSkipInvalidDataFormat() {
      // given
      String xml =
          """
          <chartdata>
          <item data="20260127|invalid|72500|71500|72200|15000000" />
          <item data="20260128|72200|73000|72000|72800|18000000" />
          </chartdata>
          """;

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).hasSize(1);
      assertThat(response.getItems().get(0).getDate()).isEqualTo("20260128");
    }

    @Test
    @DisplayName("필드 수가 부족한 데이터는 스킵")
    void shouldSkipDataWithInsufficientFields() {
      // given
      String xml =
          """
          <chartdata>
          <item data="20260127|72000|72500" />
          <item data="20260128|72200|73000|72000|72800|18000000" />
          </chartdata>
          """;

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("날짜 형식 변환 테스트")
  class DateFormatTest {

    @Test
    @DisplayName("일봉 날짜 형식 확인")
    void shouldFormatDailyDate() {
      // given
      String xml = "<item data=\"20260130|72000|72500|71500|72200|15000000\" />";

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).hasSize(1);
      assertThat(response.getItems().get(0).getDate()).isEqualTo("20260130");
    }

    @Test
    @DisplayName("분봉 날짜+시간 형식 확인")
    void shouldFormatMinuteDateTime() {
      // given
      String xml = "<item data=\"202601301430|72000|72500|71500|72200|15000000\" />";

      // when
      NaverChartResponse response = NaverChartResponse.fromXml(xml);

      // then
      assertThat(response.getItems()).hasSize(1);
      assertThat(response.getItems().get(0).getDate()).isEqualTo("202601301430");
    }
  }
}
