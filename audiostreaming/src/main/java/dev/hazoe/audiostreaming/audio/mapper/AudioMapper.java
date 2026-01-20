package dev.hazoe.audiostreaming.audio.mapper;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AudioMapper {

    private final String cdnBaseUrl;

    public AudioMapper(@Value("${app.cdn.cover-base-url}") String cdnBaseUrl) {
        this.cdnBaseUrl = cdnBaseUrl;
    }


    public AudioListItemDto toListItem(Audio audio) {
        return new AudioListItemDto(
                audio.getId(),
                audio.getTitle(),
                audio.getDurationSeconds(),
                audio.isPremium()
        );
    }

    public AudioDetailDto toDetail(Audio audio) {
        return new AudioDetailDto(
                audio.getId(),
                audio.getTitle(),
                audio.getDescription(),
                audio.getDurationSeconds(),
                buildCoverUrl(audio.getCoverPath()),
                audio.isPremium()
        );
    }

    private String buildCoverUrl(String coverPath) {
        return coverPath == null ? null : cdnBaseUrl + "/" + coverPath;
    }
}
