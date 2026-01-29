package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.response.ChartResponse;

/** 차트 데이터 제공 인터페이스 */
public interface ChartDataProvider {

  /**
   * 종목 차트 데이터 조회
   *
   * @param stockCode 종목 코드
   * @param range 기간 (1D, 1W, 1M, 3M, 1Y)
   * @return ChartResponse 차트 데이터
   */
  ChartResponse getChartData(String stockCode, String range);
}
