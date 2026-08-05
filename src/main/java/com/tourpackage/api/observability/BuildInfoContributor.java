package com.tourpackage.api.observability;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Puts the running version and profile on {@code /actuator/info}.
 *
 * <p>The first question during an incident is "which build is actually
 * deployed", and answering it from a CI log is slower and less reliable than
 * asking the process itself.
 */
@Component
public class BuildInfoContributor implements InfoContributor {

    private final String version;
    private final String profiles;

    public BuildInfoContributor(
            @Value("${app.api.version:unknown}") String version,
            @Value("${spring.profiles.active:default}") String profiles) {
        this.version = version;
        this.profiles = profiles;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("application", Map.of(
                "name", "tourpackage-api",
                "version", version,
                "profiles", profiles));
    }

}
