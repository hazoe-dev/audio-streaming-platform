package dev.hazoe.audiostreaming.audio.repository;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioRepository extends JpaRepository<Audio, Long> {

    @Query(
            value = """
                      SELECT *
                      FROM audio a
                      WHERE a.search_vector @@ to_tsquery('english', :query)
                      ORDER BY ts_rank_cd(a.search_vector, to_tsquery('english', :query)) DESC
                    """,
            countQuery = """
                      SELECT count(*)
                      FROM audio a
                      WHERE a.search_vector @@ to_tsquery('english', :query)
                    """,
            nativeQuery = true
    )
    Page<Audio> search(@Param("query") String query, Pageable pageable);

}

