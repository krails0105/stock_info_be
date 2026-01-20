package io.github.krails0105.stock_info_api;

import io.github.krails0105.stock_info_api.service.KisTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KisTokenTest {

  @Autowired private KisTokenService kisTokenService;

  @Test
  void testTokenIssuance() {
    System.out.println("=== KIS 토큰 발급 테스트 ===");

    // 첫 번째 호출: 새 토큰 발급
    String token1 = kisTokenService.getAccessToken();
    System.out.println("첫 번째 토큰: " + token1.substring(0, 20) + "...");

    // 두 번째 호출: 캐시된 토큰 사용
    String token2 = kisTokenService.getAccessToken();
    System.out.println("두 번째 토큰: " + token2.substring(0, 20) + "...");

    // 같은 토큰인지 확인
    System.out.println("토큰 동일 여부 (캐시 사용): " + token1.equals(token2));
  }
}
