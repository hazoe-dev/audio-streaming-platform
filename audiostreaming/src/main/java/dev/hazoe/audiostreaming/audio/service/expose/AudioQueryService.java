package dev.hazoe.audiostreaming.audio.service.expose;


import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AudioQueryService {
    boolean existsById(Long audioId);
    Optional<AudioDetailDto> getDetailsById(Long audioId);
    List<AudioListItemDto> getSummaryList(List<Long> audioIds);
    Page<AudioListItemDto> search(String query, Pageable pageable);
}
