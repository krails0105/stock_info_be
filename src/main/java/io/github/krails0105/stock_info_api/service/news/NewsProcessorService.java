package io.github.krails0105.stock_info_api.service.news;

import io.github.krails0105.stock_info_api.config.NewsProperties;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsImportance;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsTag;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle.ProcessingStatus;
import io.github.krails0105.stock_info_api.repository.ProcessedNewsArticleRepository;
import io.github.krails0105.stock_info_api.repository.RawNewsArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 뉴스 처리 통합 서비스.
 *
 * <p>원본 뉴스를 가져와 태깅, 클러스터링을 수행하고 처리 완료된 뉴스로 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsProcessorService {

  private final RawNewsArticleRepository rawNewsRepository;
  private final ProcessedNewsArticleRepository processedNewsRepository;
  private final NewsTaggerService taggerService;
  private final NewsDeduplicatorService deduplicatorService;
  private final NewsProperties newsProperties;

  /**
   * PENDING 상태의 원본 기사를 배치 처리한다.
   *
   * @return 처리 결과
   */
  @Transactional
  public ProcessingResult processPendingArticles() {
    int batchSize = newsProperties.getProcessing().getBatchSize();
    List<RawNewsArticle> pendingArticles =
        rawNewsRepository.findPendingArticles(PageRequest.of(0, batchSize));

    if (pendingArticles.isEmpty()) {
      log.debug("No pending articles to process");
      return new ProcessingResult(0, 0, 0);
    }

    log.info("Processing {} pending articles", pendingArticles.size());

    int processed = 0;
    int failed = 0;
    int clustered = 0;

    for (RawNewsArticle rawArticle : pendingArticles) {
      try {
        ProcessedNewsArticle result = processArticle(rawArticle);

        // 클러스터 대표 여부 카운트
        if (Boolean.TRUE.equals(result.getIsClusterRepresentative())) {
          clustered++;
        }

        // 원본 기사 상태 업데이트
        rawNewsRepository.save(rawArticle.toBuilder().status(ProcessingStatus.PROCESSED).build());

        processed++;
      } catch (Exception e) {
        log.error("Failed to process article id={}: {}", rawArticle.getId(), e.getMessage());
        rawNewsRepository.save(rawArticle.toBuilder().status(ProcessingStatus.FAILED).build());
        failed++;
      }
    }

    log.info(
        "Processing complete: processed={}, failed={}, new clusters={}",
        processed,
        failed,
        clustered);

    return new ProcessingResult(processed, failed, clustered);
  }

  /**
   * 단일 기사 처리.
   *
   * @param rawArticle 원본 기사
   * @return 처리된 기사
   */
  private ProcessedNewsArticle processArticle(RawNewsArticle rawArticle) {
    // 이미 처리된 경우 스킵
    if (processedNewsRepository.existsByRawArticleId(rawArticle.getId())) {
      log.debug("Article {} already processed, skipping", rawArticle.getId());
      throw new IllegalStateException("Article already processed");
    }

    // 1. 태깅
    List<NewsTag> tags = taggerService.assignTags(rawArticle);
    NewsImportance importance = taggerService.determineImportance(rawArticle, tags);
    String stockCode = taggerService.extractStockCode(rawArticle);
    String sectorName = taggerService.extractSectorName(rawArticle);

    // 2. 클러스터링
    NewsDeduplicatorService.ClusterAssignment clusterAssignment =
        deduplicatorService.assignCluster(rawArticle.getTitle(), rawArticle.getPublishedAt());

    // 3. 처리된 기사 생성
    ProcessedNewsArticle processedArticle =
        ProcessedNewsArticle.builder()
            .rawArticleId(rawArticle.getId())
            .title(rawArticle.getTitle())
            .publisher(rawArticle.getPublisher())
            .url(rawArticle.getUrl())
            .publishedAt(rawArticle.getPublishedAt())
            .tags(tags)
            .importance(importance)
            .stockCode(stockCode)
            .sectorName(sectorName)
            .clusterId(clusterAssignment.clusterId())
            .isClusterRepresentative(clusterAssignment.isNewCluster())
            .processedAt(LocalDateTime.now())
            .build();

    return processedNewsRepository.save(processedArticle);
  }

  /** 처리 결과. */
  public record ProcessingResult(int processed, int failed, int newClusters) {}
}
