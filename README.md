# 📊 주식 정보 API 백엔드 서버

주식 초보자를 위한 정보 제공 웹사이트의 백엔드 서버입니다.

## 🚀 프로젝트 개요

- **목표**: 주식 초보자를 위한 정보 제공 웹사이트 백엔드
- **기술 스택**: Spring Boot, WebFlux, Lombok
- **주요 기능**: 섹터별 종목 리스트, PER/PBR, 뉴스, 거래량 등 정보 제공

## 📋 구현된 API 엔드포인트

### 1. 지수 정보 API
```
GET /api/indices
```
- KOSPI, KOSDAQ 지수 정보 제공
- 현재가, 변동률, 업데이트 시간 포함

### 2. 섹터 정보 API  
```
GET /api/sectors
```
- 관심도 순으로 정렬된 섹터 리스트 제공
- 기술주, 바이오, 금융, 에너지, 소비재, 자동차 섹터 포함

### 3. 종목 정보 API
```
GET /api/stocks?sector={섹터명}
GET /api/stocks/search?keyword={검색어}
```
- 섹터별 종목 정보 제공
- 종목 검색 기능
- 현재가, PER, PBR, 거래량, 시가총액 등 포함

### 4. 뉴스 정보 API
```
GET /api/news?code={종목코드}&limit={개수}
```
- 종목별 뉴스 정보 제공
- 뉴스 제목, 요약, 발행일, URL 포함

## 🛠️ 실행 방법

### 1. 서버 실행
```bash
./gradlew bootRun
```

### 2. API 테스트
```bash
# 지수 정보 조회
curl -X GET http://localhost:8080/api/indices

# 섹터 정보 조회
curl -X GET http://localhost:8080/api/sectors

# 기술주 종목 조회
curl -X GET "http://localhost:8080/api/stocks?sector=tech"

# 삼성전자 뉴스 조회
curl -X GET "http://localhost:8080/api/news?code=005930"
```

## 🏗️ 프로젝트 구조

```
src/main/java/io/github/krails0105/stock_info_api/
├── StockInfoApiApplication.java     # 메인 애플리케이션 클래스
├── config/
│   └── WebConfig.java               # CORS 설정
├── controller/
│   ├── IndexController.java         # 지수 정보 컨트롤러
│   ├── SectorController.java        # 섹터 정보 컨트롤러
│   ├── StockController.java         # 종목 정보 컨트롤러
│   └── NewsController.java          # 뉴스 정보 컨트롤러
├── service/
│   ├── IndexService.java            # 지수 정보 서비스
│   ├── SectorService.java           # 섹터 정보 서비스
│   ├── StockService.java            # 종목 정보 서비스
│   └── NewsService.java             # 뉴스 정보 서비스
└── dto/
    ├── IndexDto.java                # 지수 정보 DTO
    ├── SectorDto.java               # 섹터 정보 DTO
    ├── StockDto.java                # 종목 정보 DTO
    └── NewsDto.java                 # 뉴스 정보 DTO
```

## 📊 응답 예시

### 지수 정보 API 응답
```json
[
  {
    "name": "KOSPI",
    "currentPrice": "2,450.30",
    "changePrice": "+15.20",
    "changeRate": "+0.62%",
    "changeDirection": "up",
    "updateTime": "2024-01-15 15:30:00"
  }
]
```

### 종목 정보 API 응답
```json
[
  {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "sector": "tech",
    "currentPrice": "75,000",
    "changePrice": "+1,000",
    "changeRate": "+1.35%",
    "changeDirection": "up",
    "volume": "15,234,567",
    "per": "15.2",
    "pbr": "1.8",
    "marketCap": "450조",
    "dividend": "2.1%",
    "updateTime": "2024-01-15 15:30:00"
  }
]
```

## 🔮 향후 개발 계획

### 1. 외부 API 연동
- [ ] KIS Developers API 연동
- [ ] Naver Finance API 연동
- [ ] Dart API 연동

### 2. 추가 기능
- [ ] 실시간 데이터 업데이트
- [ ] 데이터 캐싱 시스템
- [ ] 사용자 인증 및 즐겨찾기 기능
- [ ] 알림 시스템

### 3. 성능 최적화
- [ ] 데이터베이스 연동
- [ ] 응답 시간 최적화
- [ ] 에러 핸들링 개선

## 🎯 기술적 특징

- **Reactive Programming**: WebFlux를 활용한 비동기 처리
- **Clean Architecture**: Controller-Service-DTO 구조
- **CORS 설정**: 프론트엔드 연동을 위한 CORS 허용
- **Lombok**: 코드 간소화를 위한 어노테이션 활용

## 📝 API 문서

프로젝트가 완성되면 Swagger UI를 통한 API 문서를 제공할 예정입니다.

---

**개발자**: krails0105  
**프로젝트 시작일**: 2024-01-15  
**버전**: 1.0.0 
