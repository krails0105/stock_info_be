package io.github.krails0105.stock_info_api.service.news;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle.ProcessingStatus;
import io.github.krails0105.stock_info_api.repository.RawNewsArticleRepository;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RSS 피드에서 뉴스를 수집하는 서비스.
 *
 * <p>여러 RSS 소스에서 뉴스를 가져와 정규화 후 DB에 저장한다. URL 기반 중복 체크로 이미 수집된 뉴스는 건너뛴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectorService {

  private final RawNewsArticleRepository rawNewsRepository;

  /** Google News RSS 기본 URL */
  private static final String GOOGLE_NEWS_RSS_BASE =
      "https://news.google.com/rss/search?q=%s&hl=ko&gl=KR&ceid=KR:ko";

  /** RSS 피드 목록 (일반 + 주요 종목별) */
  private static final List<RssFeedConfig> RSS_FEEDS = buildRssFeeds();

  private static List<RssFeedConfig> buildRssFeeds() {
    List<RssFeedConfig> feeds = new ArrayList<>();

    // 1. 일반 시장 뉴스 피드
    feeds.add(
        new RssFeedConfig(
            "google-finance-kr",
            String.format(GOOGLE_NEWS_RSS_BASE, encodeQuery("주식 OR 코스피 OR 코스닥"))));
    feeds.add(
        new RssFeedConfig(
            "google-economy-kr", String.format(GOOGLE_NEWS_RSS_BASE, encodeQuery("경제 실적 배당"))));

    // 2. 시총 상위 종목별 RSS 피드
    List<String> topStocks =
        List.of(
            "삼성전자",
            "SK하이닉스",
            "LG에너지솔루션",
            "삼성바이오로직스",
            "현대차",
            "기아",
            "셀트리온",
            "KB금융",
            "신한지주",
            "NAVER",
            "카카오",
            "포스코홀딩스",
            "현대모비스",
            "LG화학",
            "삼성SDI",
            "하나금융지주",
            "삼성물산",
            "LG전자",
            "SK텔레콤",
            "두산에너빌리티");

    for (String stock : topStocks) {
      feeds.add(
          new RssFeedConfig(
              "stock-" + stock, String.format(GOOGLE_NEWS_RSS_BASE, encodeQuery(stock))));
    }

    // 3. 섹터/테마별 RSS 피드
    List<String> sectors =
        List.of(
            "반도체",
            "2차전지 배터리",
            "전기차",
            "바이오 제약",
            "AI 인공지능",
            "조선 해운",
            "철강",
            "금융 증권",
            "게임주",
            "엔터테인먼트",
            "건설 부동산",
            "통신 5G");

    for (String sector : sectors) {
      feeds.add(
          new RssFeedConfig(
              "sector-" + sector.split(" ")[0],
              String.format(GOOGLE_NEWS_RSS_BASE, encodeQuery(sector + " 주식"))));
    }

    return feeds;
  }

  private static String encodeQuery(String query) {
    return URLEncoder.encode(query, StandardCharsets.UTF_8);
  }

  /**
   * 모든 설정된 RSS 피드에서 뉴스를 수집한다.
   *
   * @return 수집 결과 (수집/중복/에러 건수)
   */
  @Transactional
  public CollectionResult collectFromAllFeeds() {
    int totalCollected = 0;
    int totalDuplicates = 0;
    int totalErrors = 0;

    log.info("Starting news collection from {} feeds", RSS_FEEDS.size());

    for (RssFeedConfig feed : RSS_FEEDS) {
      try {
        CollectionResult result = collectFromFeed(feed);
        totalCollected += result.collected();
        totalDuplicates += result.duplicates();
        log.info(
            "Feed [{}]: collected={}, duplicates={}",
            feed.name(),
            result.collected(),
            result.duplicates());
      } catch (Exception e) {
        log.error("Failed to collect from feed: {}", feed.name(), e);
        totalErrors++;
      }
    }

    log.info(
        "Collection complete: total collected={}, duplicates={}, errors={}",
        totalCollected,
        totalDuplicates,
        totalErrors);

    return new CollectionResult(totalCollected, totalDuplicates, totalErrors);
  }

  /**
   * 특정 RSS 피드에서 뉴스를 수집한다.
   *
   * @param feed 피드 설정
   * @return 수집 결과
   */
  public CollectionResult collectFromFeed(RssFeedConfig feed) throws Exception {
    SyndFeedInput input = new SyndFeedInput();
    SyndFeed syndFeed;

    try (XmlReader reader = new XmlReader(new URL(feed.url()))) {
      syndFeed = input.build(reader);
    }

    int collected = 0;
    int duplicates = 0;

    for (SyndEntry entry : syndFeed.getEntries()) {
      String url = normalizeUrl(entry.getLink());

      // URL 기반 중복 체크
      if (rawNewsRepository.existsByUrl(url)) {
        duplicates++;
        continue;
      }

      RawNewsArticle article =
          RawNewsArticle.builder()
              .title(cleanTitle(entry.getTitle()))
              .publisher(extractPublisher(entry, feed.name()))
              .url(url)
              .publishedAt(toLocalDateTime(entry.getPublishedDate()))
              .content(extractContent(entry))
              .sourceFeed(feed.name())
              .collectedAt(LocalDateTime.now())
              .status(ProcessingStatus.PENDING)
              .build();

      rawNewsRepository.save(article);
      collected++;
    }

    return new CollectionResult(collected, duplicates, 0);
  }

  /**
   * URL 정규화 (추적 파라미터 제거 등).
   *
   * @param url 원본 URL
   * @return 정규화된 URL
   */
  private String normalizeUrl(String url) {
    if (url == null) {
      return "";
    }
    // Google News redirect URL에서 실제 URL 추출 시도
    if (url.contains("news.google.com") && url.contains("url=")) {
      int startIdx = url.indexOf("url=") + 4;
      int endIdx = url.indexOf("&", startIdx);
      if (endIdx == -1) {
        endIdx = url.length();
      }
      return URLDecoder.decode(url.substring(startIdx, endIdx), StandardCharsets.UTF_8);
    }
    // 기본: 끝의 슬래시 제거
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  /**
   * 제목 정제 (HTML 태그 제거, 공백 정규화).
   *
   * @param title 원본 제목
   * @return 정제된 제목
   */
  private String cleanTitle(String title) {
    if (title == null) {
      return "";
    }
    return title
        .replaceAll("<[^>]*>", "") // HTML 태그 제거
        .replaceAll("&amp;", "&")
        .replaceAll("&lt;", "<")
        .replaceAll("&gt;", ">")
        .replaceAll("&quot;", "\"")
        .replaceAll("&#39;", "'")
        .replaceAll("\\s+", " ") // 연속 공백 제거
        .trim();
  }

  /**
   * 발행사 추출.
   *
   * @param entry RSS 엔트리
   * @param defaultSource 기본 소스명
   * @return 발행사명
   */
  private String extractPublisher(SyndEntry entry, String defaultSource) {
    // Google News는 제목에서 " - 언론사" 패턴으로 추출
    String title = entry.getTitle();
    if (title != null && title.contains(" - ")) {
      String[] parts = title.split(" - ");
      if (parts.length > 1) {
        return parts[parts.length - 1].trim();
      }
    }

    // Source 태그에서 추출
    if (entry.getSource() != null && entry.getSource().getTitle() != null) {
      return entry.getSource().getTitle();
    }

    // Author에서 추출
    if (entry.getAuthor() != null && !entry.getAuthor().isEmpty()) {
      return entry.getAuthor();
    }

    return defaultSource;
  }

  /**
   * 본문/설명 추출.
   *
   * @param entry RSS 엔트리
   * @return 본문 텍스트 (없으면 null)
   */
  private String extractContent(SyndEntry entry) {
    if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
      String content = entry.getDescription().getValue();
      // HTML 태그 제거
      return content.replaceAll("<[^>]*>", "").trim();
    }
    return null;
  }

  /**
   * Date → LocalDateTime 변환.
   *
   * @param date 원본 Date
   * @return LocalDateTime (null이면 현재 시각)
   */
  private LocalDateTime toLocalDateTime(Date date) {
    if (date == null) {
      return LocalDateTime.now();
    }
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  /** RSS 피드 설정. */
  public record RssFeedConfig(String name, String url) {}

  /** 수집 결과. */
  public record CollectionResult(int collected, int duplicates, int errors) {}
}
