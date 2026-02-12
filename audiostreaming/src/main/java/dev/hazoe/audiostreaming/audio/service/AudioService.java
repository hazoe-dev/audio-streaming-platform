package dev.hazoe.audiostreaming.audio.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.mapper.AudioMapper;
import dev.hazoe.audiostreaming.audio.service.expose.AudioQueryService;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudioService implements AudioQueryService {

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

    @Override
    public boolean existsById(Long audioId) {
        return audioRepository.existsById(audioId);
    }

    @Override
    public Optional<AudioDetailDto> getDetailsById(Long audioId) {
        return audioRepository.findById(audioId).map(audioMapper::toDetail);
    }

    @Override
    public List<AudioListItemDto> getSummaryList(List<Long> audioIds) {

        List<Audio> audios = audioRepository.findAllById(audioIds); //avoid N+1 query problem

        Map<Long, Audio> audioMap = audios.stream()
                .collect(Collectors.toMap(Audio::getId, Function.identity()));

        return audioIds.stream()
                .map(id -> Optional.ofNullable(audioMap.get(id))
                        .map(audioMapper::toListItem)
                        .orElseThrow(() -> new AudioNotFoundException(id)))
                .toList();
    }


    @Override
    public Page<AudioListItemDto> search(String query, Pageable pageable) {
        return audioRepository.search(query, pageable).map(audioMapper::toListItem);
    }
}
