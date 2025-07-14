package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.NewsDto;
import io.github.krails0105.stock_info_api.dto.SectorDto;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectorService {

  public List<SectorDto> getSectors() {
    log.info("Getting sectors information");

    // 임시 데이터 반환 (관심도 순으로 정렬된 섹터 리스트)
    return Arrays.asList(
        new SectorDto("tech", "기술주", "반도체, IT, 소프트웨어 관련 기업", 95, 150, "15.2", "1.8"),
        new SectorDto("bio", "바이오", "제약, 바이오테크놀로지 관련 기업", 90, 85, "25.4", "2.1"),
        new SectorDto("finance", "금융", "은행, 증권, 보험 관련 기업", 85, 120, "8.5", "0.6"),
        new SectorDto("energy", "에너지", "석유, 가스, 신재생에너지 관련 기업", 80, 95, "12.3", "1.2"),
        new SectorDto("consumer", "소비재", "식품, 의류, 생활용품 관련 기업", 75, 110, "18.7", "1.5"),
        new SectorDto("automotive", "자동차", "완성차, 부품, 전기차 관련 기업", 70, 80, "11.8", "0.9"));
  }

  public List<NewsDto> getNews() {
    log.info("Getting all news");

    return Arrays.asList(
        new NewsDto(
            "AAPL",
            "Apple Inc.",
            "Apple Inc. is a technology company that develops and sells smartphones, tablets, and computers.",
            "Apple Inc. is a technology company that develops and sells smartphones, tablets, and computers.",
            "Apple Inc.",
            "2024-01-01",
            "https://www.apple.com",
            "AAPL",
            "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iPhone_15_Pro_iPhone-15-Pro-Max_09122024_big.jpg"),
        new NewsDto(
            "GOOG",
            "Alphabet Inc.",
            "Alphabet Inc. is a technology company that develops and sells search engines, online advertising, and cloud computing services.",
            "Alphabet Inc. is a technology company that develops and sells search engines, online advertising, and cloud computing services.",
            "Alphabet Inc.",
            "2024-01-01",
            "https://www.google.com",
            "GOOG",
            "https://www.google.com/newsroom/images/product/google-cloud/standard/Google-Cloud-Logo_Color_2020.jpg"),
        new NewsDto(
            "MSFT",
            "Microsoft Corporation",
            "Microsoft Corporation is a technology company that develops and sells software, online services, and devices.",
            "Microsoft Corporation is a technology company that develops and sells software, online services, and devices.",
            "Microsoft Corporation",
            "2024-01-01",
            "https://www.microsoft.com",
            "MSFT",
            "https://www.microsoft.com/newsroom/images/product/windows/standard/Windows-Logo_Color_2020.jpg"));
  }

  public List<NewsDto> getNewsBySector(String sector) {
    log.info("Getting news by sector: {}", sector);
    return Arrays.asList(
        new NewsDto(
            "AAPL",
            "Apple Inc.",
            "Apple Inc. is a technology company that develops and sells smartphones, tablets, and computers.",
            "Apple Inc. is a technology company that develops and sells smartphones, tablets, and computers.",
            "Apple Inc.",
            "2024-01-01",
            "https://www.apple.com",
            "AAPL",
            "https://www.apple.com/newsroom/images/product/iphone/standard/Apple_iPhone_15_Pro_iPhone-15-Pro-Max_09122024_big.jpg"),
        new NewsDto(
            "GOOG",
            "Alphabet Inc.",
            "Alphabet Inc. is a technology company that develops and sells search engines, online advertising, and cloud computing services.",
            "Alphabet Inc. is a technology company that develops and sells search engines, online advertising, and cloud computing services.",
            "Alphabet Inc.",
            "2024-01-01",
            "https://www.google.com",
            "GOOG",
            "https://www.google.com/newsroom/images/product/google-cloud/standard/Google-Cloud-Logo_Color_2020.jpg"),
        new NewsDto(
            "MSFT",
            "Microsoft Corporation",
            "Microsoft Corporation is a technology company that develops and sells software, online services, and devices.",
            "Microsoft Corporation is a technology company that develops and sells software, online services, and devices.",
            "Microsoft Corporation",
            "2024-01-01",
            "https://www.microsoft.com",
            "MSFT",
            "https://www.microsoft.com/newsroom/images/product/windows/standard/Windows-Logo_Color_2020.jpg"));
  }
}
