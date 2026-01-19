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

**Controllers** (`controller/`): REST endpoints for sectors and stocks
- `SectorController`: `/api/sectors/**` - market summary, sector listings and details
- `StockController`: `/api/stocks/**` - stock queries, search, and rankings

**Services** (`service/`): Business logic layer
- `SectorService`: Sector data orchestration, score-based sorting, market summary calculation
- `StockService`: Stock queries, search, top rankings by score

**Providers** (`provider/`): Data access abstraction using Strategy pattern
- `SectorDataProvider` / `StockDataProvider`: Interfaces for data sources
- `MockSectorDataProvider` / `MockStockDataProvider`: Development implementations (`@Profile("local")`)
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
- `StockInfo` (Domain): 주식 기본 정보 (code, name, price, changeRate, per, pbr, market, sectorName 등)
- `StockResponse` (Response): API 응답용 (score, label, reasons 등 분석 정보 포함)
- `KrxStockItem` / `KrxStockFinancialItem` (External): KRX API 응답 매핑

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
