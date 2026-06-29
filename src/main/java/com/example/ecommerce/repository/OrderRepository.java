package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {


    // BUOC 1 cua fix: van giu nguyen method nay - day la query CO Pageable
    // (co LIMIT/OFFSET), nhung KHONG JOIN FETCH collection nao ca.
    // Chi lay ve cac cot co ban cua Order (id, ngay tao, trang thai...).
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /*
     * BUOC 2 cua fix: sau khi co duoc danh sach ID cua dung 10 order trong
     * trang hien tai (tu Buoc 1), goi tiep query nay - KHONG co LIMIT/OFFSET,
     * chi loc theo "WHERE id IN (...)" - de lay full items + product cho
     * CHINH XAC 10 order do, trong 1 lan goi DB duy nhat.
     *
     * DISTINCT can thiet vi JOIN FETCH 2 tang (items roi product) co the
     * lam nhan dong (1 order co 3 item -> SQL tra ve 3 dong cho cung 1 order).
     */
    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.items oi " +
            "JOIN FETCH oi.product " +
            "WHERE o.id IN :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);

    // Dung cho getOrderById (lay 1 don hang theo id) - khong co Pageable nen
    // JOIN FETCH thang luon duoc, khong so bi cap nhat sai trang.
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.user " +
            "JOIN FETCH o.items oi " +
            "JOIN FETCH oi.product " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
