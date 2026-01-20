package io.github.krails0105.stock_info_api;

import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.provider.StockDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@ActiveProfiles("prod")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class KisStockTest {

  private StockDataProvider stockDataProvider;

  KisStockTest(StockDataProvider stockDataProvider) {
    this.stockDataProvider = stockDataProvider;
  }

  @Test
  void testGetStockByCode() {
    System.out.println("=== KIS 주식 현재가 조회 테스트 ===");

    // 삼성전자 조회
    String code = "005930";
    System.out.println("조회 종목: " + code);

    StockScoreDto stock = stockDataProvider.getStockByCode(code);

    if (stock != null) {
      System.out.println("종목명: " + stock.getName());
      System.out.println("현재가: " + String.format("%,d", stock.getPrice()) + "원");
      System.out.println("등락률: " + stock.getPriceChange());
      System.out.println("점수: " + stock.getScore() + " (" + stock.getLabel() + ")");
      System.out.println("수익률 등급: " + stock.getReturnGrade());
      System.out.println("밸류에이션 등급: " + stock.getValuationGrade());
      System.out.println("거래량 등급: " + stock.getVolumeGrade());
      System.out.println("분석: " + stock.getReasons());
    } else {
      System.out.println("조회 실패");
    }
  }
}
