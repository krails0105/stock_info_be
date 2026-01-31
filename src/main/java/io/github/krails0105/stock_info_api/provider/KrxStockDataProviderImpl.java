package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.StockScoreDto;
import io.github.krails0105.stock_info_api.dto.domain.StockInfo;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockFinancialResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
@Slf4j
public class KrxStockDataProviderImpl implements StockDataProvider {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private final RestClient restClient;

  public KrxStockDataProviderImpl() {
    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(CONNECT_TIMEOUT)
            .withReadTimeout(READ_TIMEOUT);
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactoryBuilder.detect().build(settings);

    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public List<StockInfo> getAllStocks() {
    KrxStockFinancialResponse krxResponse = fetchKrxStockData();
    return krxResponse.getItems().stream().map(StockInfo::fromKrxFinancialItem).toList();
  }

  @Override
  public StockInfo getStockById(String stockId) {
    KrxStockFinancialResponse krxResponse = fetchKrxStockData();
    log.debug("Stock found: {}", krxResponse);
    return krxResponse.getItems().stream()
        .filter(s -> s.getStockCode().equals(stockId))
        .findFirst()
        .map(StockInfo::fromKrxFinancialItem)
        .orElse(null);
  }

  @Override
  public List<StockScoreDto> getStocksBySector(String sectorId) {
    return List.of();
  }

  @Override
  public StockScoreDto getStockByCode(String code) {
    return null;
  }

  @Override
  public List<StockScoreDto> searchStocks(String keyword) {
    return List.of();
  }

  @Override
  public List<StockScoreDto> getTopStocksBySector(String sectorId, int limit) {
    return List.of();
  }

  private KrxStockFinancialResponse fetchKrxStockData() {
    String code =
        "HDXDuwRT2eYe15H+LdVef2KPtJYOB4DNd0RiZfEw2X0RtSksuLS7Bnxpl86F7dAOLeq4x1yHv31Rs1BE2e3Ae6MM9dZFupZvytyVQZ9jrZnZvN2Hrce5tvIGLiR8s9y5B8OQ9d6t7s/rDB14nP4euh1EaJadcqRf9YjkQh0nKUA4fzZPS02rvBFmbYpTAvRGdwD7wum/aFW4tgK4ClLEJN5H+54DnIjVugDNM63c+O7XuZLf6HSF4XJ2vAxIHshN4+6Fn44l8zGYmDqMIVtilhdZx3Xdbl9EHo1GilYd0pFn7bMibk90Pcd6GSUpt3kRJW0OHp5SOJ36vltmMaa+pPlRlPUAtRyhXxw9N4xHMRSaP46lvhcuGI4r2zvdQk/X5AEGAxrvxGEeTSu7fcmLm7yUSXmUxqO8TTDyTesiy1Mof1EOegORxKB+S3Bm0h6kycQsztiES9OY9v/NyMlSHl8YLkWX26aHMuVmI7caumfchEVZ5OpuWIHm6PRejCcVnCHIKC13dsni0drKPL+rIFjtFpxqnm1GK3Z3Ny6hWpXVbO5S91neaAVNzKUq8sGi5WPj/15i4Te0eJD+lB04RVP1Uyv/Qg2DEQ1Yf+R9Q8qkCQmFR3QZ+Hhq0FD7iwy4m2QQb4/paPhuLCGWITS7KA==";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);

    byte[] responseBytes =
        restClient
            .post()
            .uri("https://data.krx.co.kr/comm/fileDn/download_csv/download.cmd")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(byte[].class);

    try {
      String csvContent = new String(Objects.requireNonNull(responseBytes), "EUC-KR");
      log.debug("KRX API Response: {}", csvContent);
      return KrxStockFinancialResponse.fromCsv(csvContent);
    } catch (Exception e) {
      log.error("Failed to parse KRX response", e);
      throw new RuntimeException("KRX 데이터 파싱 실패", e);
    }
  }
}
