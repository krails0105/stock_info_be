package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse.KrxStockItem;
import java.util.List;

/** 섹터 데이터 제공 인터페이스 나중에 DB Repository 구현체로 교체 가능 */
public interface SectorDataProvider {

  List<SectorScoreDto> getAllSectors();

  /** 특정 섹터(업종)에 속한 종목 목록 조회 - Domain DTO 반환 */
  List<StockInfo> getStocksBySectorId(String sectorId);

  List<KrxStockItem> getStocksBySectorName(String sectorId);
}
