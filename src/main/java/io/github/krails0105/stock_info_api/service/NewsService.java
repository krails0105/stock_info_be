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
                  "삼성전자, 3분기 실적 예상치 상회",
                  "삼성전자가 발표한 3분기 실적이 시장 예상치를 크게 상회했다. 메모리 반도체 가격 상승과 스마트폰 매출 증가가 주요 요인으로 분석된다."),
              new NewsDto(
                  "삼성전자, 3분기 실적 예상치 상회",
                  "삼성전자가 AI 반도체 시장에 본격적으로 진출한다고 발표했다. 엔비디아와의 경쟁이 치열해질 전망이다."),
              new NewsDto(
                  "삼성전자, 3분기 실적 예상치 상회",
                  "삼성전자가 AI 반도체 시장에 본격적으로 진출한다고 발표했다. 엔비디아와의 경쟁이 치열해질 전망이다."))
          .stream()
          .limit(limit)
          .collect(Collectors.toList());
    } else {
      // 전체 뉴스 또는 기본 데이터
      return Arrays.asList(
              new NewsDto("KOSPI 지수 상승세 지속", "KOSPI 지수가 연일 상승세를 보이고 있다..."),
              new NewsDto("바이오 섹터 주목받는 이유", "바이오 섹터가 투자자들의 관심을 끌고 있다..."))
          .stream()
          .limit(limit)
          .collect(Collectors.toList());
    }
  }
}
