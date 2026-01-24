package io.github.krails0105.stock_info_api.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 처리 완료된 뉴스 기사 엔티티.
 *
 * <p>태깅, 클러스터링이 완료된 뉴스를 저장하며, 종목/섹터 연관 정보와 중요도를 포함한다.
 */
@Entity
@Table(
    name = "processed_news_articles",
    indexes = {
      @Index(name = "idx_processed_stock", columnList = "stockCode"),
      @Index(name = "idx_processed_sector", columnList = "sectorName"),
      @Index(name = "idx_processed_cluster", columnList = "clusterId"),
      @Index(name = "idx_processed_published", columnList = "publishedAt"),
      @Index(name = "idx_processed_representative", columnList = "isClusterRepresentative")
    })
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedNewsArticle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long rawArticleId;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false, length = 100)
  private String publisher;

  @Column(nullable = false, length = 2048)
  private String url;

  @Column(nullable = false)
  private LocalDateTime publishedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "news_tags", joinColumns = @JoinColumn(name = "article_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "tag")
  private List<NewsTag> tags;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private NewsImportance importance;

  @Column(length = 20)
  private String stockCode;

  @Column(length = 50)
  private String sectorName;

  @Column(length = 50)
  private String clusterId;

  @Column private Boolean isClusterRepresentative;

  @Column(nullable = false)
  private LocalDateTime processedAt;

  public enum NewsTag {
    EARNINGS,
    CONTRACT,
    BUYBACK_DIVIDEND,
    REGULATION_RISK,
    MA,
    INDUSTRY,
    RUMOR
  }

  public enum NewsImportance {
    HIGH,
    MEDIUM,
    LOW
  }
}
