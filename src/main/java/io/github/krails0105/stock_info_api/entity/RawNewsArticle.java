package io.github.krails0105.stock_info_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수집된 원본 뉴스 기사 엔티티.
 *
 * <p>RSS 피드에서 수집한 원본 데이터를 저장하며, 처리 상태(PENDING/PROCESSED/FAILED)를 추적한다.
 */
@Entity
@Table(
    name = "raw_news_articles",
    indexes = {
      @Index(name = "idx_raw_news_url", columnList = "url", unique = true),
      @Index(name = "idx_raw_news_published", columnList = "publishedAt"),
      @Index(name = "idx_raw_news_status", columnList = "status")
    })
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RawNewsArticle {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false, length = 100)
  private String publisher;

  @Column(nullable = false, unique = true, length = 2048)
  private String url;

  @Column(nullable = false)
  private LocalDateTime publishedAt;

  @Column(length = 5000)
  private String content;

  @Column(length = 100)
  private String sourceFeed;

  @Column(nullable = false)
  private LocalDateTime collectedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProcessingStatus status;

  public enum ProcessingStatus {
    PENDING,
    PROCESSED,
    FAILED
  }
}
