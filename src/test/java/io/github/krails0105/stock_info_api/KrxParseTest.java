package io.github.krails0105.stock_info_api;

import io.github.krails0105.stock_info_api.dto.external.krx.KrxStockResponse;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class KrxParseTest {

  @Test
  void testKrxParsing() {
    RestClient restClient = RestClient.builder().build();

    String code =
        "HDXDuwRT2eYe15H+LdVef5OacEuiDpZWQr/f/k5HMOURtSksuLS7Bnxpl86F7dAOkunw9BBwugQaSjGAcH15ed4UlmGP84YYw/wfb2rAlPYtBgM+EFJCxYg3zco1gIgRZqIo4cIzoURnTI8+MmkJ4v/rk8yudrOQ53ef0cNipdpCT2QuimcLoNhc1Lfcxcp2kuAKzXEa0IBpvpB7G2ws4c0zLiPvt4cWCSl6aep8ew2uIO5+TCBkSffs+tprQzXPvTCprTIXuXT9XxFb88awpQ==";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);

    byte[] responseBytes =
        restClient
            .post()
            .uri("https://data.krx.co.kr/comm/fileDn/download_csv/download.cmd")
            .header(
                HttpHeaders.REFERER, "http://data.krx.co.kr/comm/fileDn/GenerateOTP/generate.cmd")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(byte[].class);

    try {
      String csvContent = new String(Objects.requireNonNull(responseBytes), "EUC-KR");

      System.out.println("=== Raw CSV (first 500 chars) ===");
      System.out.println(csvContent.substring(0, Math.min(500, csvContent.length())));

      System.out.println("\n=== Parsing CSV ===");
      KrxStockResponse response = KrxStockResponse.fromCsv(csvContent);

      System.out.println("Total items: " + response.getItems().size());
      System.out.println("\n=== First 3 items ===");
      response.getItems().stream()
          .limit(3)
          .forEach(
              item -> {
                System.out.println(
                    item.getStockCode()
                        + " | "
                        + item.getStockName()
                        + " | "
                        + item.getSectorName()
                        + " | "
                        + item.getChangeRate()
                        + "%");
              });

      System.out.println("\n=== Sectors ===");
      response.groupBySector().keySet().stream().limit(10).forEach(System.out::println);

    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
