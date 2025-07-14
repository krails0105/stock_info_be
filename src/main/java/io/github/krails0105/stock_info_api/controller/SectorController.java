package io.github.krails0105.stock_info_api.controller;

import io.github.krails0105.stock_info_api.dto.SectorDto;
import io.github.krails0105.stock_info_api.service.SectorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
@Slf4j
public class SectorController {

  private final SectorService sectorService;

  @GetMapping
  public List<SectorDto> getSectors() {
    log.info("Request to get sectors");
    return sectorService.getSectors();
  }
}
