package ru.practicum.shareit.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.item.id = :itemId")
    List<Comment> findByItemId(@Param("itemId") Long itemId);

    List<Comment> findByItemIdIn(List<Long> itemIds);
}