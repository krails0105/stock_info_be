package io.github.krails0105.stock_info_api.scheduler;

import io.github.krails0105.stock_info_api.config.NewsProperties;
import io.github.krails0105.stock_info_api.service.news.NewsCollectorService;
import io.github.krails0105.stock_info_api.service.news.NewsProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집/처리 스케줄러.
 *
 * <p>주기적으로 RSS 피드에서 뉴스를 수집하고, PENDING 상태 기사를 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

  private final NewsCollectorService collectorService;
  private final NewsProcessorService processorService;
  private final NewsProperties newsProperties;

  /**
   * 뉴스 수집 (기본 15분 주기).
   *
   * <p>application.yml의 news.collection.interval-minutes로 조정 가능.
   */
  @Scheduled(fixedRateString = "${news.collection.interval-minutes:15}000")
  public void collectNews() {
    if (!newsProperties.getCollection().isEnabled()) {
      log.debug("News collection is disabled");
      return;
    }

    log.info("Starting scheduled news collection");
    try {
      NewsCollectorService.CollectionResult result = collectorService.collectFromAllFeeds();
      log.info(
          "Scheduled collection complete: collected={}, duplicates={}, errors={}",
          result.collected(),
          result.duplicates(),
          result.errors());
    } catch (Exception e) {
      log.error("Scheduled collection failed", e);
    }
  }

  /**
   * 뉴스 처리 (기본 5분 주기).
   *
   * <p>application.yml의 news.processing.interval-minutes로 조정 가능.
   */
  @Scheduled(fixedRateString = "${news.processing.interval-minutes:5}000")
  public void processNews() {
    if (!newsProperties.getCollection().isEnabled()) {
      log.debug("News processing is disabled (collection disabled)");
      return;
    }

    log.debug("Starting scheduled news processing");
    try {
      NewsProcessorService.ProcessingResult result = processorService.processPendingArticles();
      if (result.processed() > 0 || result.failed() > 0) {
        log.info(
            "Scheduled processing complete: processed={}, failed={}, newClusters={}",
            result.processed(),
            result.failed(),
            result.newClusters());
      }
    } catch (Exception e) {
      log.error("Scheduled processing failed", e);
    }
  }
}
