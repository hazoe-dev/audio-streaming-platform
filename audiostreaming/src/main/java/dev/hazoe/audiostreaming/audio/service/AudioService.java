package dev.hazoe.audiostreaming.audio.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.mapper.AudioMapper;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudioService {

    private final AudioRepository audioRepository;
    private final AudioMapper audioMapper;

    public Page<AudioListItemDto> getAudios(Pageable pageable) {
        return audioRepository.findAll(pageable)
                .map(audioMapper::toListItem);
    }

    public AudioDetailDto getAudioDetail(Long id) {
        Audio audio = audioRepository.findById(id)
                .orElseThrow(() -> new AudioNotFoundException(id));
        return audioMapper.toDetail(audio);
    }
}
