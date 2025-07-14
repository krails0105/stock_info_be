package io.github.krails0105.stock_info_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsDto {
  private String newsId; // 뉴스 ID
  private String title; // 뉴스 제목
  private String summary; // 뉴스 요약
  private String content; // 뉴스 내용
  private String author; // 작성자
  private String publishDate; // 발행일
  private String url; // 뉴스 URL
  private String stockCode; // 관련 종목코드
  private String imageUrl; // 뉴스 이미지 URL
}
