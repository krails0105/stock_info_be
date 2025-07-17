package io.github.krails0105.stock_info_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsDto {
  private String title; // 뉴스 제목
  private String content; // 뉴스 내용
}
