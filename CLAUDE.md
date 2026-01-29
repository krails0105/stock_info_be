# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

```bash
# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Build project
./gradlew build

# Code formatting (Spotless with Google Java Format)
./gradlew spotlessApply    # Auto-format code
./gradlew spotlessCheck    # Check formatting without applying
```

## Architecture Overview

This is a Spring Boot REST API for stock market information. The architecture follows a layered pattern:

```
Controller → Service → Provider (Data Access) → DTO
```

### Key Components

**Controllers** (`controller/`): REST endpoints for sectors, stocks, and indexes
- `SectorController`: `/api/sectors/**` - market summary, sector listings and details
- `StockController`: `/api/stocks/**` - stock queries, search, and rankings
- `IndexController`: `/api/indexes` - 시장 지수 조회 (코스피, 코스닥)

**Services** (`service/`): Business logic layer
- `SectorService`: Sector data orchestration, score-based sorting, market summary calculation
- `StockService`: Stock queries, search, top rankings by score
- `IndexService`: 지수 조회 (TARGET_INDEXES Set으로 필터링)
- `NewsAggregatorService`: 뉴스 집계, 태깅, 클러스터링, DB 조회

**News Services** (`service/news/`): 뉴스 수집/처리 파이프라인
- `NewsCollectorService`: RSS 피드 수집 (Google News 주식/경제)
- `NewsTaggerService`: 키워드 기반 태그 부여 (7종), 중요도 결정
- `NewsDeduplicatorService`: Jaccard 유사도 기반 클러스터링
- `NewsProcessorService`: 수집 → 태깅 → 클러스터링 통합 파이프라인

**Scheduler** (`scheduler/`): 주기적 작업
- `NewsScheduler`: 뉴스 수집 (15분), 처리 (5분) 스케줄러

**Entity** (`entity/`): JPA 엔티티
- `RawNewsArticle`: 원본 뉴스 (PENDING/PROCESSED/FAILED 상태)
- `ProcessedNewsArticle`: 처리된 뉴스 (태그, 중요도, 클러스터ID)

**Repository** (`repository/`): JPA 레포지토리
- `RawNewsArticleRepository`: URL 중복 체크, 상태별 조회
- `ProcessedNewsArticleRepository`: 종목/섹터별, 클러스터 대표 조회

**Providers** (`provider/`): Data access abstraction using Strategy pattern
- `SectorDataProvider` / `StockDataProvider` / `IndexDataProvider`: Interfaces for data sources
- `ChartDataProvider`: 차트 데이터 인터페이스 (MockChartDataProvider, KrxChartDataProvider)
- `MockSectorDataProvider` / `MockStockDataProvider`: Development implementations (`@Profile("local")`)
- `KrxStockDataProviderImpl`: KRX API integration
- `KrxIndexDataProviderImpl`: KRX 지수 API (INDEX_CODES Map으로 관리)
- `KisStockDataProviderImpl`: KIS API integration (in progress)

**DTOs** (`dto/`): Data transfer objects with Lombok annotations
- `StockScoreDto` / `SectorScoreDto`: Core entities with scores (0-100) and labels (STRONG/NEUTRAL/WEAK)
- `ScoreboardResponse`: Main dashboard response with market summary and hot sectors

### DTO Layer Separation

DTO는 역할에 따라 3가지로 분리:

```
External API/DB → Provider → Service → Controller → Client
       ↓              ↓          ↓           ↓
  External DTO    Domain DTO   Domain    Response DTO
```

| DTO 종류 | 위치 | 역할 |
|---------|------|------|
| **External DTO** | `dto/external/` | 외부 API 응답 매핑 (KRX, KIS) |
| **Domain DTO** | `dto/domain/` | 내부 비즈니스 로직용 표준 형식 |
| **Response DTO** | `dto/response/` | 클라이언트 응답용 (가공된 데이터) |

**변환 규칙:**
- Provider: External DTO → Domain DTO 변환
- Service: Domain DTO만 반환 (표현 계층 의존 X)
- Controller: Domain DTO → Response DTO 변환

**주요 DTO:**
- `StockInfo` (Domain): 주식 기본 정보 (code, name, price, priceChange, changeRate, per, pbr, market, sectorName, marketCap 등)
- `Index` (Domain): 지수 정보 (name, closingPrice, priceChange, changeRate, openingPrice, highPrice, lowPrice 등)
- `StockDetailResponse` (Response): 종목 상세 조회용 (stockCode, stockName, closingPrice, eps, per, bps, pbr 등)
- `ChartResponse` (Response): 차트 데이터 응답 (dataPoints, meta)
- `StockListItem` (Response): 목록 조회용 - `fromStockInfo()` 메서드로 변환
- `IndexResponse` (Response): 지수 응답용 - `fromIndex()` 메서드로 변환
- `KrxStockItem` / `KrxStockFinancialItem` / `KrxIndexResponse` (External): KRX API 응답 매핑

### Key Endpoints

**SectorController:**
- `GET /api/sectors` - 전체 섹터 목록 (점수순)
- `GET /api/sectors/{sectorName}/stocks` - 섹터별 종목 목록

**StockController:**
- `GET /api/stocks/{id}` - 종목 상세 (KrxStockFinancialItem 반환)
- `GET /api/stocks/sector/{sectorId}` - 섹터별 종목 리스트
- `GET /api/stocks/search?keyword=` - 종목 검색
- `GET /api/stocks/top?limit=` - 상위 종목

**IndexController:**
- `GET /api/indexes` - 시장 지수 조회 (코스피, 코스닥)

### Scoring System

Stocks and sectors use a 0-100 score system:
- STRONG: 70-100
- NEUTRAL: 40-69
- WEAK: 0-39

Each score includes explanatory reasons for beginner users.

## Code Style

- Google Java Format enforced via Spotless
- No wildcard imports (custom Spotless rule)
- Lombok annotations: `@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- Run `./gradlew spotlessApply` before committing

## Configuration

- Main config: `src/main/resources/application.yml`
- Server runs on port 8080
- KIS API credentials via environment variables: `KIS_APP_KEY`, `KIS_APP_SECRET`, `KIS_ACCOUNT_NUMBER`
- Spring profile `local` activates mock data providers
- Spring profile `prod` activates KRX real data providers

### News Configuration (`news.*`)
```yaml
news:
  collection:
    enabled: true          # 수집 활성화
    interval-minutes: 15   # 수집 주기
  processing:
    batch-size: 100        # 배치 처리 크기
    interval-minutes: 5    # 처리 주기
  clustering:
    similarity-threshold: 0.6  # Jaccard 유사도 임계값
    window-hours: 72           # 클러스터링 윈도우
```

### Running with Profiles
```bash
# Local (Mock data)
./gradlew bootRun

# Production (KRX real data)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

## 작업 완료 체크리스트

작업 완료 시 아래 순서대로 진행:

1. **코드 포맷팅 및 커밋**
   ```bash
   ./gradlew spotlessApply
   git add .
   git commit -m "feat: 작업 내용"
   git push
   ```

2. **문서 업데이트**
   - `../docs/PROGRESS.md` 작업 내역 추가
   - 필요시 이 파일(CLAUDE.md) 업데이트

3. **PR 생성** (feature 브랜치인 경우)
   ```bash
   gh pr create --title "제목" --body "## Summary\n- 내용"
   ```
