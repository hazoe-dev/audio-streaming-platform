package dev.hazoe.audiostreaming.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AudioStorageException extends RuntimeException {

    public AudioStorageException(String message) {
        super(message);
    }

    public AudioStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
