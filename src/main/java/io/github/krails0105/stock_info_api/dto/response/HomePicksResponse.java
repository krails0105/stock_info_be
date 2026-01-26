package io.github.krails0105.stock_info_api.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 홈 Watchlist Picks API 응답 DTO */
@Getter
@Builder
public class HomePicksResponse {
  private LocalDateTime asOf;
  private String preset;
  private List<StockPickCard> items;
}
