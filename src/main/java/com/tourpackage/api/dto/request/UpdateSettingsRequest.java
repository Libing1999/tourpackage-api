package com.tourpackage.api.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

/**
 * Settings are edited as a screenful at a time, so they're saved that way:
 * a map of key to new value. Keys that aren't already in the table are
 * rejected rather than created — settings are defined by migrations, and
 * letting the UI invent them would leave rows nothing reads.
 */
public record UpdateSettingsRequest(

        @NotEmpty(message = "At least one setting is required")
        Map<String, String> values

) {
}
