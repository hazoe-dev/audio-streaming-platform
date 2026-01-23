package dev.hazoe.audiostreaming.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidProgressPositionException extends RuntimeException {

    private final int positionSeconds;
    private final int durationSeconds;

    public InvalidProgressPositionException(int positionSeconds, int durationSeconds) {
        super(String.format(
                "Position %d exceeds audio duration %d",
                positionSeconds,
                durationSeconds
        ));
        this.positionSeconds = positionSeconds;
        this.durationSeconds = durationSeconds;
    }

    public int getPositionSeconds() {
        return positionSeconds;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }
}
