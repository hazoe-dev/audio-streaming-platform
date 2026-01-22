package dev.hazoe.audiostreaming.library.repository;

import dev.hazoe.audiostreaming.library.domain.LibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {

    @Query("""
                select li from LibraryItem li
                join fetch li.audio
                where li.userId = :userId
            """)
    List<LibraryItem> findByUserIdWithAudio(Long userId);

    boolean existsByUserIdAndAudio_Id(Long userId, Long audioId);

    void deleteByUserIdAndAudio_Id(Long userId, Long audioId);
}
