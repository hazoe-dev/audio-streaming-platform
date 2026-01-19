package dev.hazoe.audiostreaming.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AudioNotFoundException extends RuntimeException {

    public AudioNotFoundException(Long audioId) {
        super("Audio not found with id: " + audioId);
    }
}
