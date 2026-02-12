package dev.hazoe.audiostreaming.audio.service.expose;


import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;

import java.util.Optional;

public interface AudioQueryService {
    boolean existsById(Long audioId);
    Optional<AudioDetailDto> getDetailsById(Long audioId);

}
