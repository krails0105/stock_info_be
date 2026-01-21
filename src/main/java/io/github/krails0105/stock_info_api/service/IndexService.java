package io.github.krails0105.stock_info_api.service;

import io.github.krails0105.stock_info_api.dto.domain.Index;
import io.github.krails0105.stock_info_api.provider.IndexDataProvider;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndexService {

  private static final Set<String> TARGET_INDEXES = Set.of("코스피", "코스닥");

  private final IndexDataProvider indexDataProvider;

  public List<Index> getIndexes() {
    return indexDataProvider.getIndexes().stream()
        .filter(i -> TARGET_INDEXES.contains(i.getName()))
        .toList();
  }
}
