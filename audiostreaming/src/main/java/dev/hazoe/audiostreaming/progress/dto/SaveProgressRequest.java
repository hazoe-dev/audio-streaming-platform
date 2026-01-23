package dev.hazoe.audiostreaming.progress.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaveProgressRequest (
        @NotNull Long audioId,
        @Min(0) int positionSeconds
){
}
