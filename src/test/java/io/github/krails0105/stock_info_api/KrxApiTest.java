package io.github.krails0105.stock_info_api;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class KrxApiTest {

  @Test
  void testKrxCsvDownload() {
    RestClient restClient = RestClient.builder().build();

    String code =
        "HDXDuwRT2eYe15H+LdVef5OacEuiDpZWQr/f/k5HMOURtSksuLS7Bnxpl86F7dAOkunw9BBwugQaSjGAcH15ed4UlmGP84YYw/wfb2rAlPYtBgM+EFJCxYg3zco1gIgRZqIo4cIzoURnTI8+MmkJ4v/rk8yudrOQ53ef0cNipdpCT2QuimcLoNhc1Lfcxcp2kuAKzXEa0IBpvpB7G2ws4c0zLiPvt4cWCSl6aep8ew2uIO5+TCBkSffs+tprQzXPvTCprTIXuXT9XxFb88awpQ==";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);

    String response =
        restClient
            .post()
            .uri("https://data.krx.co.kr/comm/fileDn/download_csv/download.cmd")
            .header(
                HttpHeaders.REFERER, "http://data.krx.co.kr/comm/fileDn/GenerateOTP/generate.cmd")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(String.class);

    // EUC-KR로 인코딩된 경우 변환 필요할 수 있음
    System.out.println("=== KRX API Response ===");
    System.out.println(response);
  }

  @Test
  void testKrxCsvDownloadWithBytes() {
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
            .header(
                HttpHeaders.COOKIE,
                "JSESSIONID=240wQ49DV8VaA7ULIYwoCsgy4DsrNb1DWVHhq1ZamKLJ8B1Mt3sIHE8TXmL1Bfdv.bWRjX2RvbWFpbi9tZGNvd2FwMi1tZGNhcHAwMQ==; __smVisitorID=zBTvpginDKc")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(byte[].class);

    // KRX는 EUC-KR 인코딩을 사용하므로 변환
    String response = new String(responseBytes, StandardCharsets.UTF_8);
    System.out.println("=== KRX API Response (UTF-8) ===");
    System.out.println(response);

    // EUC-KR로 디코딩 시도
    try {
      String eucKrResponse = new String(responseBytes, "EUC-KR");
      System.out.println("\n=== KRX API Response (EUC-KR) ===");
      System.out.println(eucKrResponse);
    } catch (Exception e) {
      System.out.println("EUC-KR decoding failed: " + e.getMessage());
    }
  }

  @Test
  void testGetAllStocks() {
    RestClient restClient = RestClient.builder().build();

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
      System.out.println(responseBytes);
      String csvContent = new String(Objects.requireNonNull(responseBytes), "EUC-KR");
      System.out.println("KRX API Response: " + csvContent);
      //      return csvContent;
      //      return KrxStockResponse.fromCsv(csvContent);
    } catch (Exception e) {
      System.out.println("Failed to parse KRX response " + e);
      throw new RuntimeException("KRX 데이터 파싱 실패", e);
    }
  }
}
