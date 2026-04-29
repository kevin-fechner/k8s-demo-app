package com.demo.orderservice.repository;

import com.demo.orderservice.entity.Order;
import com.demo.orderservice.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query(
      """
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE (:cursor IS NULL OR o.id > :cursor)
            AND (CAST(:status AS string) IS NULL OR o.status = :status)
            AND (:customerEmail IS NULL OR LOWER(o.customerEmail)
                 LIKE LOWER(CONCAT('%', CAST(:customerEmail AS string), '%')))
            AND (CAST(:fromDate AS timestamp) IS NULL OR o.createdAt >= :fromDate)
            AND (CAST(:toDate AS timestamp) IS NULL OR o.createdAt <= :toDate)
            ORDER BY o.id ASC
            """)
  List<Order> findWithCursor(
      @Param("cursor") Long cursor,
      @Param("status") OrderStatus status,
      @Param("customerEmail") String customerEmail,
      @Param("fromDate") LocalDateTime fromDate,
      @Param("toDate") LocalDateTime toDate,
      Pageable pageable);

  @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
  Optional<Order> findByIdWithItems(Long id);
}
