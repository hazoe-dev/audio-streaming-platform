package dev.hazoe.audiostreaming.library.repository;

import dev.hazoe.audiostreaming.library.domain.LibraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {

    List<LibraryItem> findByUserId(Long userId);

    boolean existsByUserIdAndAudioId(Long userId, Long audioId);

    void deleteByUserIdAndAudioId(Long userId, Long audioId);

}
