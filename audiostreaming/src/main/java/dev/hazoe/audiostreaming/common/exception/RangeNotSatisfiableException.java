package dev.hazoe.audiostreaming.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
public class RangeNotSatisfiableException extends RuntimeException {

    private final long fileSize;

    public RangeNotSatisfiableException(String message, long fileSize) {
        super(message);
        this.fileSize = fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }
}