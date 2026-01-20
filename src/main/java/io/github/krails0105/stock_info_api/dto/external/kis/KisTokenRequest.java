package io.github.krails0105.stock_info_api.dto.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * KIS OAuth 토큰 발급 요청 DTO
 *
 * <p>[요청 엔드포인트] POST /oauth2/tokenP
 *
 * <p>[요청 예시] { "grant_type": "client_credentials", "appkey": "앱키", "appsecret": "앱시크릿" }
 */
@Getter
@Builder
public class KisTokenRequest {

  /** 인증 타입 (항상 "client_credentials" 고정) */
  @JsonProperty("grant_type")
  @Builder.Default
  private String grantType = "client_credentials";

  /** KIS에서 발급받은 앱 키 */
  @JsonProperty("appkey")
  private String appKey;

  /** KIS에서 발급받은 앱 시크릿 */
  @JsonProperty("appsecret")
  private String appSecret;
}
