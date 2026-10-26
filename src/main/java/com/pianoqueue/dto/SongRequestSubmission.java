package com.pianoqueue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SongRequestSubmission(
        @NotBlank @Size(max = 50) String requesterName,
        @NotBlank @Size(max = 200) String trackName,
        @Size(max = 200) String artistName,
        @Size(max = 500) String artworkUrl,
        boolean namePublic
) {}
