package dev.hazoe.audiostreaming.progress.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.audio.service.expose.AudioQueryService;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.common.exception.InvalidProgressPositionException;
import dev.hazoe.audiostreaming.progress.domain.ListeningProgress;
import dev.hazoe.audiostreaming.progress.dto.ListeningProgressResponse;
import dev.hazoe.audiostreaming.progress.repository.ListeningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListeningProgressService {
    private final ListeningProgressRepository progressRepository;
    private final AudioQueryService audioQueryService;

    @Transactional
    public void saveProgress(Long userId, Long audioId, int positionSeconds) {

        AudioDetailDto audio = audioQueryService.getDetailsById(audioId)
                .orElseThrow(() -> new AudioNotFoundException(audioId));

        if (positionSeconds > audio.durationSeconds()) {
            throw new InvalidProgressPositionException(positionSeconds, audio.durationSeconds());
        }

        ListeningProgress progress =
                progressRepository.findByUserIdAndAudioId(userId, audioId)
                        .orElseGet(() -> {
                            ListeningProgress p =
                                    new ListeningProgress(userId, audioId, 0);
                            return progressRepository.save(p); //  persist ONCE
                        });

        progress.updatePosition(positionSeconds);
    }


    @Transactional(readOnly = true)
    public ListeningProgressResponse getProgress(Long userId, Long audioId) {
        if (!audioQueryService.existsById(audioId)) {
            throw new AudioNotFoundException(audioId);
        }

        return progressRepository.findByUserIdAndAudioId(userId, audioId)
                .map(progress -> new ListeningProgressResponse(
                        audioId,
                        progress.getPositionSeconds()
                ))
                .orElse(new ListeningProgressResponse(audioId, 0));
    }
}
