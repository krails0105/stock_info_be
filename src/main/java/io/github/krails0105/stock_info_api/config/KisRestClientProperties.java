package io.github.krails0105.stock_info_api.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ============================================================================
 * KIS(한국투자증권) OpenAPI 연동을 위한 설정 프로퍼티 클래스
 * ============================================================================
 *
 * [@ConfigurationProperties란?]
 * application.yml 파일의 설정값을 자바 객체에 자동으로 바인딩해주는 어노테이션
 *
 * 예를 들어 application.yml에 아래와 같이 설정하면:
 *   rest-client:
 *     kis:
 *       base-url: https://openapi.koreainvestment.com:9443
 *       connect-timeout: 5000
 *
 * 이 클래스의 baseUrl 필드에 URL이, connectTimeout 필드에 5000이 자동 주입됨
 *
 * [왜 이 방식을 사용하나요?]
 * - 타입 안전성: 문자열로 설정값을 가져오는 것보다 안전함
 * - IDE 자동완성: 설정값을 사용할 때 자동완성이 됨
 * - 유효성 검사: @Validated와 함께 사용하면 설정값 검증 가능
 * - 환경별 설정: 개발/운영 환경에 따라 다른 값을 쉽게 주입
 *
 * [환경변수 활용]
 * 민감한 정보(API 키 등)는 yml 파일에 직접 쓰지 않고 환경변수로 주입:
 *   app-key: ${KIS_APP_KEY:}  -> KIS_APP_KEY 환경변수 값 사용, 없으면 빈 문자열
 */
@Getter // Lombok: 모든 필드의 getter 메서드 자동 생성
@Setter // Lombok: 모든 필드의 setter 메서드 자동 생성 (설정값 바인딩에 필요)
@Component // Spring Bean으로 등록 (다른 클래스에서 @Autowired로 주입받을 수 있음)
@ConfigurationProperties(prefix = "rest-client.kis") // yml에서 rest-client.kis 하위 설정을 바인딩
public class KisRestClientProperties {

  /*
   * KIS OpenAPI 기본 URL
   * - 실전투자: https://openapi.koreainvestment.com:9443
   * - 모의투자: https://openapivts.koreainvestment.com:29443
   */
  @NotBlank private String baseUrl;
  private int connectTimeout;
  private int readTimeout;
  private String appKey;
  private String appSecret;
  private String accountNumber;
}
