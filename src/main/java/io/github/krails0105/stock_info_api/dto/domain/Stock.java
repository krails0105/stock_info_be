package io.github.krails0105.stock_info_api.dto.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * 주식 기본 정보 도메인 DTO
 *
 * <p>외부 API(KRX, KIS)에서 가져온 데이터를 내부에서 사용하는 표준 형식 비즈니스 로직에서 사용하는 핵심 도메인 객체
 */
@Getter
@Builder
public class Stock {

  /** 종목코드 (예: "005930") */
  private String code;

  /** 종목명 (예: "삼성전자") */
  private String name;

  /** 시장구분 (예: "KOSPI", "KOSDAQ") */
  private String market;

  /** 업종명 (예: "전기전자") */
  private String sectorName;

  /** 현재가/종가 (단위: 원) */
  private long price;

  /** 전일 대비 가격 변동 (단위: 원) */
  private long priceChange;

  /** 등락률 (%, 예: 2.5, -1.3) */
  private double changeRate;

  /** 시가총액 (단위: 원) */
  private long marketCap;

  /** 거래량 */
  private long volume;

  /** PER (주가수익비율) */
  private Double per;

  /** PBR (주가순자산비율) */
  private Double pbr;
}
