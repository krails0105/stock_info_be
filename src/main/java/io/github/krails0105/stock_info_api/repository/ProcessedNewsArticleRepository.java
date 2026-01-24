package io.github.krails0105.stock_info_api.repository;

import io.github.krails0105.stock_info_api.entity.ProcessedNewsArticle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 처리 완료된 뉴스 기사 저장소. */
@Repository
public interface ProcessedNewsArticleRepository extends JpaRepository<ProcessedNewsArticle, Long> {

  /**
   * 종목 관련 뉴스 조회 (특정 시점 이후).
   *
   * @param stockCode 종목 코드
   * @param after 조회 시작 시점
   * @return 뉴스 목록
   */
  List<ProcessedNewsArticle> findByStockCodeAndPublishedAtAfter(
      String stockCode, LocalDateTime after);

  /**
   * 섹터 관련 뉴스 조회 (특정 시점 이후).
   *
   * @param sectorName 섹터명
   * @param after 조회 시작 시점
   * @return 뉴스 목록
   */
  List<ProcessedNewsArticle> findBySectorNameAndPublishedAtAfter(
      String sectorName, LocalDateTime after);

  /**
   * 클러스터 대표 뉴스 조회.
   *
   * @param clusterId 클러스터 ID
   * @return 대표 뉴스
   */
  @Query(
      "SELECT p FROM ProcessedNewsArticle p "
          + "WHERE p.clusterId = :clusterId AND p.isClusterRepresentative = true")
  Optional<ProcessedNewsArticle> findClusterRepresentative(@Param("clusterId") String clusterId);

  /**
   * 최근 뉴스 조회 (중요도/발행일 정렬).
   *
   * @param since 조회 시작 시점
   * @param pageable 페이징 정보
   * @return 뉴스 목록
   */
  @Query(
      "SELECT p FROM ProcessedNewsArticle p "
          + "WHERE p.publishedAt > :since "
          + "ORDER BY p.importance ASC, p.publishedAt DESC")
  List<ProcessedNewsArticle> findRecentNews(@Param("since") LocalDateTime since, Pageable pageable);

  /**
   * 클러스터 대표 뉴스만 조회 (종목별).
   *
   * @param stockCode 종목 코드
   * @param after 조회 시작 시점
   * @return 대표 뉴스 목록
   */
  @Query(
      "SELECT p FROM ProcessedNewsArticle p "
          + "WHERE p.stockCode = :stockCode "
          + "AND p.publishedAt > :after "
          + "AND p.isClusterRepresentative = true "
          + "ORDER BY p.importance ASC, p.publishedAt DESC")
  List<ProcessedNewsArticle> findRepresentativeNewsByStockCode(
      @Param("stockCode") String stockCode, @Param("after") LocalDateTime after);

  /**
   * 클러스터 대표 뉴스만 조회 (섹터별).
   *
   * @param sectorName 섹터명
   * @param after 조회 시작 시점
   * @return 대표 뉴스 목록
   */
  @Query(
      "SELECT p FROM ProcessedNewsArticle p "
          + "WHERE p.sectorName = :sectorName "
          + "AND p.publishedAt > :after "
          + "AND p.isClusterRepresentative = true "
          + "ORDER BY p.importance ASC, p.publishedAt DESC")
  List<ProcessedNewsArticle> findRepresentativeNewsBySectorName(
      @Param("sectorName") String sectorName, @Param("after") LocalDateTime after);

  /**
   * 원본 기사 ID로 처리 완료 여부 확인.
   *
   * @param rawArticleId 원본 기사 ID
   * @return 존재 여부
   */
  boolean existsByRawArticleId(Long rawArticleId);
}
