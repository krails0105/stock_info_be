package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.dto.StockListResponse;
import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.provider.SectorDataProvider;
import io.github.krails0105.stock_info_api.provider.StockDataProvider;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

  private final StockDataProvider stockDataProvider;
  private final SectorDataProvider sectorDataProvider;

  public StockListResponse getStocksBySector(String sectorId) {
    // 섹터 정보 조회 (getAllSectors에서 필터링)
    SectorScoreDto sector =
        sectorDataProvider.getAllSectors().stream()
            .filter(s -> s.getSectorId().equals(sectorId))
            .findFirst()
            .orElse(null);

    if (sector == null) {
      return null;
    }

    List<StockScoreDto> stocks =
        stockDataProvider.getStocksBySector(sectorId).stream()
            .sorted(Comparator.comparingInt(StockScoreDto::getScore).reversed())
            .toList();

    return StockListResponse.builder()
        .sectorId(sector.getSectorId())
        .sectorName(sector.getSectorName())
        .sectorScore(sector.getScore())
        .sectorLabel(sector.getLabel())
        .stocks(stocks)
        .totalCount(stocks.size())
        .build();
  }

  public StockInfo getStockById(String id) {
    return stockDataProvider.getStockById(id);
  }

  public StockScoreDto getStockByCode(String code) {
    return stockDataProvider.getStockByCode(code);
  }

  public List<StockScoreDto> searchStocks(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return List.of();
    }
    return stockDataProvider.searchStocks(keyword.trim());
  }

  public List<StockScoreDto> getTopStocks(int limit) {
    return sectorDataProvider.getAllSectors().stream()
        .flatMap(sector -> stockDataProvider.getTopStocksBySector(sector.getSectorId(), 3).stream())
        .sorted(Comparator.comparingInt(StockScoreDto::getScore).reversed())
        .limit(limit)
        .toList();
  }
}
