package io.github.krails0105.stock_info_api.provider;

import io.github.krails0105.stock_info_api.dto.domain.Index;
import java.util.List;

public interface IndexDataProvider {

  List<Index> getIndexes();
}
