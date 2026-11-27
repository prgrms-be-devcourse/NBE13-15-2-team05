package com.team5.auction.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.team5.auction.category.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    @Query("select count(a) from Auction a where a.category.id = :categoryId")
    long countAuctionsByCategoryId(@Param("categoryId") Long categoryId);

}
