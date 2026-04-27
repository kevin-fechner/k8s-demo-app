package com.demo.productservice.repository;

import com.demo.productservice.dto.StockResponse;
import com.demo.productservice.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
            SELECT p FROM Product p
            WHERE (:cursor IS NULL OR p.id > :cursor)
            AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            AND (:inStock IS NULL OR (:inStock = true AND p.stock > 0)
                                  OR (:inStock = false AND p.stock = 0))
            ORDER BY p.id ASC
            """)
    List<Product> findWithCursor(
            @Param("cursor") Long cursor,
            @Param("name") String name,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStock") Boolean inStock,
            Pageable pageable
    );

    @Query(value = """
            SELECT new com.demo.productservice.dto.StockResponse(
               COUNT(DISTINCT p.id),
               SUM(CASE WHEN p.stock > 0 THEN 1 ELSE 0 END),
               SUM(CASE WHEN p.stock = 0 THEN 1 ELSE 0 END))
            FROM Product p
            """)
    StockResponse getStockNumbers();
}