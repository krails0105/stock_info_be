package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.NewsDto;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

  public List<NewsDto> getNews(String code, int limit) {
    log.info("Getting news for stock code: {}, limit: {}", code, limit);

    // 임시 데이터 반환 (나중에 외부 API 연동으로 대체)
    if ("005930".equals(code)) {
      return Arrays.asList(
              new NewsDto(
                  "news1",
                  "삼성전자, 3분기 실적 예상치 상회",
                  "삼성전자가 3분기 실적에서 시장 예상치를 상회하는 성과를 기록했다...",
                  "삼성전자가 발표한 3분기 실적이 시장 예상치를 크게 상회했다. 메모리 반도체 가격 상승과 스마트폰 매출 증가가 주요 요인으로 분석된다.",
                  "경제뉴스",
                  "2024-01-15 09:00:00",
                  "https://example.com/news1",
                  "005930",
                  "https://example.com/image1.jpg"),
              new NewsDto(
                  "news2",
                  "삼성전자, AI 반도체 시장 진출 본격화",
                  "삼성전자가 AI 반도체 시장에 본격적으로 진출한다고 발표했다...",
                  "삼성전자가 AI 반도체 시장에 본격적으로 진출한다고 발표했다. 엔비디아와의 경쟁이 치열해질 전망이다.",
                  "기술뉴스",
                  "2024-01-15 14:30:00",
                  "https://example.com/news2",
                  "005930",
                  "https://example.com/image2.jpg"))
          .stream()
          .limit(limit)
          .collect(Collectors.toList());
    } else {
      // 전체 뉴스 또는 기본 데이터
      return Arrays.asList(
              new NewsDto(
                  "news3",
                  "KOSPI 지수 상승세 지속",
                  "KOSPI 지수가 연일 상승세를 보이고 있다...",
                  "KOSPI 지수가 외국인 매수세와 함께 상승세를 지속하고 있다.",
                  "시장뉴스",
                  "2024-01-15 16:00:00",
                  "https://example.com/news3",
                  "",
                  "https://example.com/image3.jpg"),
              new NewsDto(
                  "news4",
                  "바이오 섹터 주목받는 이유",
                  "바이오 섹터가 투자자들의 관심을 끌고 있다...",
                  "바이오 섹터가 신약 개발 성과와 함께 투자자들의 관심을 끌고 있다.",
                  "섹터뉴스",
                  "2024-01-15 11:15:00",
                  "https://example.com/news4",
                  "",
                  "https://example.com/image4.jpg"))
          .stream()
          .limit(limit)
          .collect(Collectors.toList());
    }
  }
}
