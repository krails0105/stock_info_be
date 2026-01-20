package io.github.krails0105.stock_info_api;

import io.github.krails0105.stock_info_api.config.KisRestClientProperties;
import io.github.krails0105.stock_info_api.service.KisTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@SpringBootTest
class KisRawResponseTest {

  @Autowired private RestClient kisRestClient;
  @Autowired private KisRestClientProperties props;
  @Autowired private KisTokenService kisTokenService;

  @Test
  void testRawResponse() {
    String token = kisTokenService.getAccessToken();
    String code = "005930";

    String response =
        kisRestClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", code)
                        .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .header("appkey", props.getAppKey())
            .header("appsecret", props.getAppSecret())
            .header("tr_id", "FHKST01010100")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);

    System.out.println("=== KIS API Raw Response ===");
    System.out.println(response);
  }
}
