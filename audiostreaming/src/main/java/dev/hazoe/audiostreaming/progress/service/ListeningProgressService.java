package dev.hazoe.audiostreaming.progress.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.common.exception.InvalidProgressPositionException;
import dev.hazoe.audiostreaming.progress.domain.ListeningProgress;
import dev.hazoe.audiostreaming.progress.repository.ListeningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListeningProgressService {
    private final ListeningProgressRepository progressRepository;
    private final AudioRepository audioRepository;

    @Transactional
    public void saveProgress(Long userId, Long audioId, int positionSeconds) {

        Audio audio = audioRepository.findById(audioId)
                .orElseThrow(() -> new AudioNotFoundException(audioId));

        if (positionSeconds > audio.getDurationSeconds()) {
            throw new InvalidProgressPositionException(positionSeconds, audio.getDurationSeconds());
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


}
