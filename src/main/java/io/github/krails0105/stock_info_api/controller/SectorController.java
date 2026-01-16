package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.ScoreboardResponse;
import io.github.krails0105.stock_info_api.dto.SectorScoreDto;
import io.github.krails0105.stock_info_api.service.SectorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
@Slf4j
public class SectorController {

  private final SectorService sectorService;

  /** 홈 현황판 API - Hot Sectors TOP3 + 전체 섹터 리스트 + 시장 요약 */
  @GetMapping("/scoreboard")
  public ScoreboardResponse getScoreboard() {
    log.info("Request to get scoreboard");
    return sectorService.getScoreboard();
  }

  /** 전체 섹터 리스트 (점수순 정렬) */
  @GetMapping
  public List<SectorScoreDto> getAllSectors() {
    log.info("Request to get all sectors");
    return sectorService.getAllSectors();
  }

  /** 특정 섹터 상세 조회 */
  @GetMapping("/{sectorId}")
  public ResponseEntity<SectorScoreDto> getSectorById(@PathVariable String sectorId) {
    log.info("Request to get sector: {}", sectorId);
    SectorScoreDto sector = sectorService.getSectorById(sectorId);
    if (sector == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(sector);
  }
}
