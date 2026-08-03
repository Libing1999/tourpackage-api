package com.tourpackage.api.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

/**
 * New ordering as a list of ids. Position in the list is the new
 * {@code display_order}, so the client sends what it shows rather than
 * computing index numbers the server would have to trust anyway.
 */
public record ReorderRequest(

        @NotEmpty(message = "At least one id is required")
        List<UUID> ids

) {
}
