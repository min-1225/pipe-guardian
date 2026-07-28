package com.pipeguardian.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PipeRequest(
        @NotBlank String pipeCode,
        String location,
        @Positive Double outerDiameterMm
) {}
