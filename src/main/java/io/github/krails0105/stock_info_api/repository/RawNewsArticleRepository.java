package io.github.krails0105.stock_info_api.repository;

import io.github.krails0105.stock_info_api.entity.RawNewsArticle;
import io.github.krails0105.stock_info_api.entity.RawNewsArticle.ProcessingStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** 원본 뉴스 기사 저장소. */
@Repository
public interface RawNewsArticleRepository extends JpaRepository<RawNewsArticle, Long> {

  /**
   * URL 중복 체크.
   *
   * @param url 확인할 URL
   * @return 존재 여부
   */
  boolean existsByUrl(String url);

  /**
   * 처리 상태별 조회.
   *
   * @param status 처리 상태
   * @return 해당 상태의 기사 목록
   */
  List<RawNewsArticle> findByStatus(ProcessingStatus status);

  /**
   * PENDING 상태 기사 조회 (수집 시간순).
   *
   * @param pageable 페이징 정보
   * @return PENDING 기사 목록
   */
  @Query("SELECT r FROM RawNewsArticle r WHERE r.status = 'PENDING' ORDER BY r.collectedAt ASC")
  List<RawNewsArticle> findPendingArticles(Pageable pageable);

  /**
   * 특정 피드 소스의 최근 기사 수 조회.
   *
   * @param sourceFeed 피드 소스명
   * @return 기사 수
   */
  long countBySourceFeed(String sourceFeed);
}
