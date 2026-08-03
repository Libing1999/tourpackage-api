package com.tourpackage.api.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** No {@code updated_at} — see {@link PackageInclude}. */
@Entity
@Table(name = "package_excludes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageExclude {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "icon", length = 80)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
