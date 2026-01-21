package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.domain.Index;
import io.github.krails0105.stock_info_api.dto.external.krx.KrxIndexResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class KrxIndexDataProviderImpl implements IndexDataProvider {

  private static final Map<String, String> INDEX_CODES =
      Map.of(
          "KOSPI",
          "CrXSsBNNRCu7sKFjyLfMv7ovbbaQu2MCFXZxvQUUmHYRtSksuLS7Bnxpl86F7dAOljmd3W5WSuvBZefxIBXis5aKeNlDwhhhErMpnuHql0qLt4WN28I81i+7KB+smpuTkmi2DsJVi7nd9V9czE0E2sIg8AtAjFopL5enDBGlMAotVU1yKYe5tbbTljkhZJ2UVmu7dxwt3VTOS253NhIUz6gmScDRQAumnWS+nVj25PQFLUVRogm6XveqXBqi9mWvXj/MGcl3zlhm1+3U+0XOY4pPgztr2I3IM5kl8ywGPaU=",
          "KOSDAQ",
          "CrXSsBNNRCu7sKFjyLfMvyIkiXj+uh47Lx3oEBwV/zERtSksuLS7Bnxpl86F7dAOljmd3W5WSuvBZefxIBXiswkSE+XScf5+02zIjMlosK6Lt4WN28I81i+7KB+smpuTkmi2DsJVi7nd9V9czE0E2sIg8AtAjFopL5enDBGlMAotVU1yKYe5tbbTljkhZJ2UVmu7dxwt3VTOS253NhIUz6gmScDRQAumnWS+nVj25PQFLUVRogm6XveqXBqi9mWvXj/MGcl3zlhm1+3U+0XOY4pPgztr2I3IM5kl8ywGPaU=");

  private final RestClient restClient;

  public KrxIndexDataProviderImpl() {
    this.restClient = RestClient.builder().build();
  }

  @Override
  public List<Index> getIndexes() {
    return INDEX_CODES.values().stream()
        .flatMap(code -> fetchKrxData(code).getItems().stream())
        .map(Index::fromKrxIndexItem)
        .toList();
  }

  private KrxIndexResponse fetchKrxData(String code) {
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
      return KrxIndexResponse.fromCsv(csvContent);
    } catch (Exception e) {
      log.error("Failed to parse KRX response", e);
      throw new RuntimeException("KRX 데이터 파싱 실패", e);
    }
  }
}
