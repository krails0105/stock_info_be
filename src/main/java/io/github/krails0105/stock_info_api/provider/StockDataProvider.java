package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import java.util.List;

/** 종목 데이터 제공 인터페이스 나중에 DB Repository 구현체로 교체 가능 */
public interface StockDataProvider {

  List<StockInfo> getAllStocks();

  /**
   * 종목 상세 조회 (Domain DTO 반환)
   *
   * @param stockId 종목 코드
   * @return StockInfo 도메인 객체 (재무 정보 포함)
   */
  StockInfo getStockById(String stockId);

  List<StockScoreDto> getStocksBySector(String sectorId);

  StockScoreDto getStockByCode(String code);

  List<StockScoreDto> searchStocks(String keyword);

  List<StockScoreDto> getTopStocksBySector(String sectorId, int limit);
}
