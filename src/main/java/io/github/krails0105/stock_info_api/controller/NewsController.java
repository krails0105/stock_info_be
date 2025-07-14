package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.NewsDto;
import io.github.krails0105.stock_info_api.service.NewsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

  private final NewsService newsService;

  @GetMapping
  public List<NewsDto> getNews(
      @RequestParam(required = false) String code, @RequestParam(defaultValue = "10") int limit) {
    log.info("Request to get news - code: {}, limit: {}", code, limit);
    return newsService.getNews(code, limit);
  }
}
