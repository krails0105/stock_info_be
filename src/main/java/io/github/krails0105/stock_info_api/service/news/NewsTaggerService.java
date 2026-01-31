package io.github.krails0105.stock_info_api.service.news;

import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsImportance;
import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle.NewsTag;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 뉴스 태깅 서비스.
 *
 * <p>키워드 기반 규칙으로 뉴스에 태그와 중요도를 부여한다. 향후 LLM 기반 태깅으로 확장 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsTaggerService {

  /** 태그별 키워드 패턴. */
  private static final Map<NewsTag, List<Pattern>> TAG_PATTERNS =
      Map.of(
          NewsTag.EARNINGS,
          List.of(
              Pattern.compile("실적", Pattern.CASE_INSENSITIVE),
              Pattern.compile("분기", Pattern.CASE_INSENSITIVE),
              Pattern.compile("영업이익", Pattern.CASE_INSENSITIVE),
              Pattern.compile("순이익", Pattern.CASE_INSENSITIVE),
              Pattern.compile("매출", Pattern.CASE_INSENSITIVE),
              Pattern.compile("어닝[스]?", Pattern.CASE_INSENSITIVE),
              Pattern.compile("earnings", Pattern.CASE_INSENSITIVE)),
          NewsTag.CONTRACT,
          List.of(
              Pattern.compile("계약", Pattern.CASE_INSENSITIVE),
              Pattern.compile("수주", Pattern.CASE_INSENSITIVE),
              Pattern.compile("납품", Pattern.CASE_INSENSITIVE),
              Pattern.compile("공급", Pattern.CASE_INSENSITIVE)),
          NewsTag.BUYBACK_DIVIDEND,
          List.of(
              Pattern.compile("배당", Pattern.CASE_INSENSITIVE),
              Pattern.compile("자사주", Pattern.CASE_INSENSITIVE),
              Pattern.compile("매입", Pattern.CASE_INSENSITIVE),
              Pattern.compile("주주환원", Pattern.CASE_INSENSITIVE)),
          NewsTag.REGULATION_RISK,
          List.of(
              Pattern.compile("규제", Pattern.CASE_INSENSITIVE),
              Pattern.compile("제재", Pattern.CASE_INSENSITIVE),
              Pattern.compile("과징금", Pattern.CASE_INSENSITIVE),
              Pattern.compile("수사", Pattern.CASE_INSENSITIVE),
              Pattern.compile("검찰", Pattern.CASE_INSENSITIVE),
              Pattern.compile("소송", Pattern.CASE_INSENSITIVE)),
          NewsTag.MA,
          List.of(
              Pattern.compile("인수", Pattern.CASE_INSENSITIVE),
              Pattern.compile("합병", Pattern.CASE_INSENSITIVE),
              Pattern.compile("M&A", Pattern.CASE_INSENSITIVE),
              Pattern.compile("M\\s*&\\s*A", Pattern.CASE_INSENSITIVE)),
          NewsTag.INDUSTRY,
          List.of(
              Pattern.compile("업종", Pattern.CASE_INSENSITIVE),
              Pattern.compile("산업", Pattern.CASE_INSENSITIVE),
              Pattern.compile("섹터", Pattern.CASE_INSENSITIVE),
              Pattern.compile("시장", Pattern.CASE_INSENSITIVE)),
          NewsTag.RUMOR,
          List.of(
              Pattern.compile("루머", Pattern.CASE_INSENSITIVE),
              Pattern.compile("소문", Pattern.CASE_INSENSITIVE),
              Pattern.compile("찌라시", Pattern.CASE_INSENSITIVE),
              Pattern.compile("관측", Pattern.CASE_INSENSITIVE)));

  /** 신뢰할 수 있는 언론사 목록 (신선도 가산점용). */
  private static final List<String> TRUSTED_PUBLISHERS =
      List.of("연합뉴스", "한국경제", "매일경제", "조선비즈", "서울경제", "머니투데이", "뉴스1");

  /** 6자리 주식 코드 패턴. */
  private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

  /** 섹터명 키워드 매핑 (KRX 업종명 기준) - 확장 버전. */
  private static final Map<String, List<String>> SECTOR_KEYWORDS =
      Map.ofEntries(
          Map.entry(
              "전기·전자",
              List.of(
                  "반도체", "메모리", "파운드리", "HBM", "D램", "낸드", "전자", "디스플레이", "OLED", "LCD", "AP",
                  "시스템반도체", "비메모리", "DDR5", "GPU")),
          Map.entry(
              "운송장비·부품",
              List.of(
                  "자동차", "전기차", "EV", "완성차", "현대차", "기아", "자율주행", "배터리팩", "모빌리티", "수소차", "하이브리드",
                  "전장부품")),
          Map.entry(
              "제약",
              List.of(
                  "바이오", "제약", "신약", "임상", "의약", "헬스케어", "CMO", "CDMO", "mRNA", "세포치료제", "항암제",
                  "면역항암", "바이오시밀러")),
          Map.entry("은행", List.of("은행", "금융", "대출", "예금", "저축", "금리", "여신", "수신")),
          Map.entry("증권", List.of("증권사", "자산운용", "투자", "펀드", "IB", "IPO", "유상증자", "공모주")),
          Map.entry("보험", List.of("보험", "생명보험", "손해보험", "재보험", "보험료")),
          Map.entry(
              "IT 서비스",
              List.of(
                  "AI", "인공지능", "소프트웨어", "클라우드", "플랫폼", "게임", "챗GPT", "LLM", "데이터센터", "SaaS", "핀테크",
                  "메타버스", "IT서비스")),
          Map.entry(
              "전기·가스",
              List.of(
                  "에너지", "태양광", "풍력", "친환경", "2차전지", "배터리", "ESS", "신재생", "탄소중립", "수소", "연료전지",
                  "전력")),
          Map.entry(
              "화학",
              List.of("석유", "정유", "화학", "리튬", "양극재", "음극재", "전해액", "분리막", "석유화학", "NCC", "에틸렌")),
          Map.entry(
              "기계·장비",
              List.of(
                  "조선업", "선박", "HD현대중공업", "한화오션", "기계", "삼성중공업", "LNG선", "컨테이너선", "방산", "항공우주",
                  "로봇")),
          Map.entry("금속", List.of("철강", "포스코", "금속", "비철금속", "알루미늄", "구리", "희토류")),
          Map.entry("통신", List.of("통신", "5G", "6G", "이통사", "KT", "SKT", "LGU+", "네트워크")),
          Map.entry(
              "유통",
              List.of(
                  "유통", "소비재", "화장품", "백화점", "이커머스", "쿠팡", "네이버쇼핑", "편의점", "마트", "K뷰티", "면세점")));

  /** 주요 종목명 → 종목코드 매핑 (시총 상위 종목 + 주요 관심 종목) - 확장 버전. */
  private static final Map<String, String> STOCK_NAME_CODE_MAP =
      Map.ofEntries(
          // 시총 상위 종목
          Map.entry("삼성전자", "005930"),
          Map.entry("삼성", "005930"),
          Map.entry("SK하이닉스", "000660"),
          Map.entry("하이닉스", "000660"),
          Map.entry("LG에너지솔루션", "373220"),
          Map.entry("LG엔솔", "373220"),
          Map.entry("삼성바이오로직스", "207940"),
          Map.entry("삼성바이오", "207940"),
          Map.entry("현대차", "005380"),
          Map.entry("현대자동차", "005380"),
          Map.entry("기아", "000270"),
          Map.entry("기아차", "000270"),
          Map.entry("셀트리온", "068270"),
          Map.entry("KB금융", "105560"),
          Map.entry("KB금융지주", "105560"),
          Map.entry("신한지주", "055550"),
          Map.entry("신한금융", "055550"),
          Map.entry("NAVER", "035420"),
          Map.entry("네이버", "035420"),
          Map.entry("카카오", "035720"),
          Map.entry("포스코홀딩스", "005490"),
          Map.entry("포스코", "005490"),
          Map.entry("POSCO", "005490"),
          Map.entry("현대모비스", "012330"),
          Map.entry("LG화학", "051910"),
          Map.entry("삼성SDI", "006400"),
          Map.entry("SK이노베이션", "096770"),
          Map.entry("SK이노", "096770"),
          Map.entry("삼성물산", "028260"),
          Map.entry("하나금융지주", "086790"),
          Map.entry("하나금융", "086790"),
          Map.entry("삼성생명", "032830"),
          Map.entry("LG전자", "066570"),
          Map.entry("SK텔레콤", "017670"),
          Map.entry("SKT", "017670"),
          Map.entry("KT", "030200"),
          Map.entry("케이티", "030200"),
          Map.entry("SK", "034730"),
          Map.entry("두산에너빌리티", "034020"),
          Map.entry("두산에너", "034020"),
          Map.entry("한화에어로스페이스", "012450"),
          Map.entry("한화에어로", "012450"),
          Map.entry("크래프톤", "259960"),
          Map.entry("HD현대중공업", "329180"),
          Map.entry("현대중공업", "329180"),
          Map.entry("엔씨소프트", "036570"),
          Map.entry("NC소프트", "036570"),
          Map.entry("넷마블", "251270"),
          Map.entry("카카오뱅크", "323410"),
          Map.entry("SK바이오팜", "326030"),
          Map.entry("LG생활건강", "051900"),
          Map.entry("아모레퍼시픽", "090430"),
          Map.entry("아모레", "090430"),
          Map.entry("HLB", "028300"),
          Map.entry("에코프로비엠", "247540"),
          Map.entry("에코프로BM", "247540"),
          Map.entry("에코프로", "086520"),
          // 추가 종목 (시총 상위 확대)
          Map.entry("삼성화재", "000810"),
          Map.entry("우리금융지주", "316140"),
          Map.entry("우리금융", "316140"),
          Map.entry("LG", "003550"),
          Map.entry("한화솔루션", "009830"),
          Map.entry("삼성에스디에스", "018260"),
          Map.entry("삼성SDS", "018260"),
          Map.entry("한국전력", "015760"),
          Map.entry("한전", "015760"),
          Map.entry("삼성전기", "009150"),
          Map.entry("SK스퀘어", "402340"),
          Map.entry("HD한국조선해양", "009540"),
          Map.entry("한국조선해양", "009540"),
          Map.entry("한화오션", "042660"),
          Map.entry("삼성중공업", "010140"),
          Map.entry("LG이노텍", "011070"),
          Map.entry("SK온", "361610"),
          Map.entry("카카오페이", "377300"),
          Map.entry("토스뱅크", "024110"),
          Map.entry("대한항공", "003490"),
          Map.entry("SK네트웍스", "001740"),
          Map.entry("CJ제일제당", "097950"),
          Map.entry("CJ", "001040"),
          Map.entry("한미약품", "128940"),
          Map.entry("유한양행", "000100"),
          Map.entry("녹십자", "006280"),
          Map.entry("SK바이오사이언스", "302440"));

  /**
   * 뉴스에 태그를 부여한다.
   *
   * @param article 원본 기사
   * @return 부여된 태그 목록
   */
  public List<NewsTag> assignTags(RawNewsArticle article) {
    List<NewsTag> tags = new ArrayList<>();
    String text = buildSearchText(article);

    for (Map.Entry<NewsTag, List<Pattern>> entry : TAG_PATTERNS.entrySet()) {
      for (Pattern pattern : entry.getValue()) {
        if (pattern.matcher(text).find()) {
          tags.add(entry.getKey());
          break;
        }
      }
    }

    if (tags.isEmpty()) {
      tags.add(NewsTag.INDUSTRY);
    }

    log.debug("Assigned tags {} to article: {}", tags, article.getTitle());
    return tags;
  }

  /**
   * 뉴스 중요도를 결정한다.
   *
   * @param article 원본 기사
   * @param tags 부여된 태그
   * @return 중요도
   */
  public NewsImportance determineImportance(RawNewsArticle article, List<NewsTag> tags) {
    int score = 0;

    // 신선도 점수 (24시간 이내: +3, 72시간 이내: +1)
    if (article.getPublishedAt() != null) {
      long hoursAgo = ChronoUnit.HOURS.between(article.getPublishedAt(), LocalDateTime.now());
      if (hoursAgo <= 24) {
        score += 3;
      } else if (hoursAgo <= 72) {
        score += 1;
      }
    }

    // 태그별 점수
    if (tags.contains(NewsTag.EARNINGS)) {
      score += 3;
    }
    if (tags.contains(NewsTag.CONTRACT)) {
      score += 2;
    }
    if (tags.contains(NewsTag.MA)) {
      score += 3;
    }
    if (tags.contains(NewsTag.REGULATION_RISK)) {
      score += 2;
    }
    if (tags.contains(NewsTag.BUYBACK_DIVIDEND)) {
      score += 2;
    }

    // 신뢰 언론사 가산점
    if (isTrustedPublisher(article.getPublisher())) {
      score += 1;
    }

    NewsImportance importance;
    if (score >= 5) {
      importance = NewsImportance.HIGH;
    } else if (score >= 2) {
      importance = NewsImportance.MEDIUM;
    } else {
      importance = NewsImportance.LOW;
    }

    log.debug(
        "Determined importance {} (score={}) for article: {}",
        importance,
        score,
        article.getTitle());
    return importance;
  }

  /**
   * 종목 코드 추출 (종목명 매핑 + 6자리 숫자 패턴).
   *
   * @param article 원본 기사
   * @return 종목 코드 (없으면 null)
   */
  public String extractStockCode(RawNewsArticle article) {
    String text = buildSearchText(article);

    // 1. 종목명으로 찾기 (매핑 테이블 사용)
    for (Map.Entry<String, String> entry : STOCK_NAME_CODE_MAP.entrySet()) {
      if (text.contains(entry.getKey())) {
        log.debug(
            "Found stock {} ({}) in article: {}",
            entry.getKey(),
            entry.getValue(),
            article.getTitle());
        return entry.getValue();
      }
    }

    // 2. 6자리 숫자 패턴 찾기 (한국 주식 코드)
    Matcher matcher = STOCK_CODE_PATTERN.matcher(text);
    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }

  /**
   * 섹터명 추출 (키워드 기반).
   *
   * @param article 원본 기사
   * @return 섹터명 (없으면 null)
   */
  public String extractSectorName(RawNewsArticle article) {
    String text = buildSearchText(article);

    for (Map.Entry<String, List<String>> entry : SECTOR_KEYWORDS.entrySet()) {
      for (String keyword : entry.getValue()) {
        if (text.contains(keyword)) {
          log.debug(
              "Found sector {} (keyword: {}) in article: {}",
              entry.getKey(),
              keyword,
              article.getTitle());
          return entry.getKey();
        }
      }
    }

    return null;
  }

  private String buildSearchText(RawNewsArticle article) {
    StringBuilder sb = new StringBuilder();
    if (article.getTitle() != null) {
      sb.append(article.getTitle()).append(" ");
    }
    if (article.getContent() != null) {
      sb.append(article.getContent());
    }
    return sb.toString();
  }

  private boolean isTrustedPublisher(String publisher) {
    if (publisher == null) {
      return false;
    }
    return TRUSTED_PUBLISHERS.stream().anyMatch(publisher::contains);
  }
}
