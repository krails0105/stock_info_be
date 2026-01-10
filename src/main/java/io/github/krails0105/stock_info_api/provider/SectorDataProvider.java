package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import java.util.List;

/** 섹터 데이터 제공 인터페이스 나중에 DB Repository 구현체로 교체 가능 */
public interface SectorDataProvider {

  List<SectorScoreDto> getAllSectors();

  SectorScoreDto getSectorById(String sectorId);
}
