package io.github.krails0105.stock_info_api.dto.insight;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectorInsight {

  private final SectorEntity entity;
  private final InsightMeta meta;
  private final SectorSummary summary;
  private final List<TopPick> topPicks;
  private final InsightNews news;

  // P0-1: 표본 크기 정보
  private final Integer sampleSize;
  private final Boolean lowSampleWarning;

  // P0-3: 동적 섹션 타이틀
  private final String sectionTitle;

  @Getter
  @Builder
  public static class SectorEntity {
    private final String type = "SECTOR";
    private final String name;
  }

  @Getter
  @Builder
  public static class SectorSummary {
    private final String headline;
    private final List<String> drivers;
  }

  @Getter
  @Builder
  public static class TopPick {

    public enum PickType {
      STABLE,
      VALUE,
      GROWTH,
      MOMENTUM,
      WATCH
    }

    // P0-3: TopPick 역할 구분
    public enum TopPickRole {
      REPRESENTATIVE, // 섹터 대표 종목
      WATCHLIST_PRIORITY // 우선 관찰 종목
    }

    private final String code;
    private final String name;
    private final ScoreLabel grade;
    private final PickType pickType;
    private final List<String> reasons;
    private final String caution;

    // P0-3: 역할 필드
    private final TopPickRole role;
  }
}
