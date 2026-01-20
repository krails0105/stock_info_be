package io.github.krails0105.stock_info_api.dto.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * ============================================================================
 * KIS OAuth 토큰 발급 응답 DTO
 * ============================================================================
 *
 * [DTO(Data Transfer Object)란?]
 * 계층 간 데이터 전송을 위한 객체
 * 외부 API 응답을 자바 객체로 변환(역직렬화)할 때 사용
 *
 * [OAuth 2.0 인증 흐름]
 * 1. 클라이언트가 앱키/시크릿으로 토큰 요청
 * 2. 인증 서버가 access_token 발급
 * 3. 이후 API 호출 시 토큰을 헤더에 포함
 * 4. 토큰 만료 시 재발급
 *
 * [@JsonProperty란?]
 * JSON 필드명과 자바 필드명이 다를 때 매핑을 지정
 * 예: JSON의 "access_token" -> 자바의 accessToken
 *
 * [KIS API 토큰 발급 요청 예시]
 * POST /oauth2/tokenP
 * {
 *   "grant_type": "client_credentials",
 *   "appkey": "앱키",
 *   "appsecret": "앱시크릿"
 * }
 *
 * [응답 예시]
 * {
 *   "access_token": "eyJ0eXAiOiJKV1...",
 *   "access_token_token_expired": "2024-01-01 12:00:00",
 *   "token_type": "Bearer",
 *   "expires_in": 86400
 * }
 */
@Getter // Lombok: 모든 필드의 getter 자동 생성
@NoArgsConstructor // Lombok: 기본 생성자 자동 생성 (JSON 역직렬화에 필요)
public class KisTokenResponse {

  /*
   * 액세스 토큰
   * - API 호출 시 Authorization 헤더에 "Bearer {토큰}" 형태로 사용
   * - JWT(JSON Web Token) 형식
   */
  @JsonProperty("access_token") // JSON의 "access_token"을 이 필드에 매핑
  private String accessToken;

  /*
   * 토큰 만료 일시
   * - 형식: "2024-01-01 12:00:00"
   * - 이 시간이 지나면 토큰이 무효화됨
   */
  @JsonProperty("access_token_token_expired")
  private String accessTokenExpired;

  /*
   * 토큰 타입
   * - 보통 "Bearer"
   * - Authorization 헤더 형식: "Bearer {토큰}"
   */
  @JsonProperty("token_type")
  private String tokenType;

  /*
   * 토큰 유효 시간 (초)
   * - 예: 86400 = 24시간
   * - 이 시간이 지나면 토큰 재발급 필요
   */
  @JsonProperty("expires_in")
  private int expiresIn;
}
