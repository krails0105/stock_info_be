package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.config.KisRestClientProperties;
import io.github.krails0105.stock_info_api.dto.external.kis.KisTokenRequest;
import io.github.krails0105.stock_info_api.dto.external.kis.KisTokenResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * KIS OAuth 토큰 관리 서비스
 *
 * <p>[역할]
 *
 * <ul>
 *   <li>토큰 발급 요청 (POST /oauth2/tokenP)
 *   <li>토큰 캐싱 (만료 전까지 재사용)
 *   <li>토큰 만료 시 자동 재발급
 * </ul>
 *
 * <p>[토큰 유효 시간]
 *
 * <ul>
 *   <li>KIS 토큰은 약 24시간(86400초) 유효
 *   <li>만료 1시간 전에 미리 재발급하여 안정성 확보
 * </ul>
 */
@Service
@Slf4j
public class KisTokenService {

  private final RestClient kisRestClient;
  private final KisRestClientProperties properties;

  /** 캐싱된 토큰 */
  private String cachedToken;

  /** 토큰 만료 시간 */
  private LocalDateTime tokenExpireTime;

  /** 만료 전 여유 시간 (1시간 전에 미리 갱신) */
  private static final int REFRESH_MARGIN_MINUTES = 60;

  public KisTokenService(RestClient kisRestClient, KisRestClientProperties properties) {
    this.kisRestClient = kisRestClient;
    this.properties = properties;
  }

  /**
   * 유효한 액세스 토큰 반환
   *
   * <p>캐싱된 토큰이 유효하면 재사용, 만료되었거나 없으면 새로 발급
   *
   * @return Bearer 토큰 문자열
   */
  public String getAccessToken() {
    if (isTokenValid()) {
      log.debug("캐싱된 토큰 사용 (만료: {})", tokenExpireTime);
      return cachedToken;
    }

    log.info("토큰 발급 요청 시작");
    return refreshToken();
  }

  /**
   * 토큰이 유효한지 확인
   *
   * @return 토큰이 존재하고 만료 시간 전이면 true
   */
  private boolean isTokenValid() {
    if (cachedToken == null || tokenExpireTime == null) {
      return false;
    }
    // 만료 1시간 전에 미리 갱신
    return LocalDateTime.now().plusMinutes(REFRESH_MARGIN_MINUTES).isBefore(tokenExpireTime);
  }

  /**
   * 새 토큰 발급
   *
   * @return 새로 발급받은 액세스 토큰
   */
  private synchronized String refreshToken() {
    // 동시 요청 시 중복 발급 방지를 위한 재확인
    if (isTokenValid()) {
      return cachedToken;
    }

    KisTokenRequest request =
        KisTokenRequest.builder()
            .appKey(properties.getAppKey())
            .appSecret(properties.getAppSecret())
            .build();

    log.debug("토큰 발급 요청: appKey={}", maskString(properties.getAppKey()));

    KisTokenResponse response =
        kisRestClient
            .post()
            .uri("/oauth2/tokenP")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(KisTokenResponse.class);

    if (response == null || response.getAccessToken() == null) {
      throw new RuntimeException("KIS 토큰 발급 실패: 응답이 없습니다");
    }

    cachedToken = response.getAccessToken();
    tokenExpireTime = parseExpireTime(response.getAccessTokenExpired());

    log.info("토큰 발급 성공 (만료: {}, 유효시간: {}초)", tokenExpireTime, response.getExpiresIn());

    return cachedToken;
  }

  /**
   * 만료 시간 문자열 파싱
   *
   * @param expiredStr "2024-01-01 12:00:00" 형식의 문자열
   * @return LocalDateTime 객체
   */
  private LocalDateTime parseExpireTime(String expiredStr) {
    if (expiredStr == null || expiredStr.isBlank()) {
      // 만료 시간이 없으면 23시간 후로 설정 (안전 마진)
      return LocalDateTime.now().plusHours(23);
    }

    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      return LocalDateTime.parse(expiredStr, formatter);
    } catch (Exception e) {
      log.warn("토큰 만료 시간 파싱 실패: {}, 기본값 23시간 사용", expiredStr);
      return LocalDateTime.now().plusHours(23);
    }
  }

  /**
   * 문자열 마스킹 (로깅용)
   *
   * @param str 원본 문자열
   * @return 앞 4자리만 보이고 나머지는 * 처리
   */
  private String maskString(String str) {
    if (str == null || str.length() <= 4) {
      return "****";
    }
    return str.substring(0, 4) + "****";
  }

  /** 캐시 초기화 (테스트용) */
  public void clearCache() {
    cachedToken = null;
    tokenExpireTime = null;
    log.info("토큰 캐시 초기화됨");
  }
}
