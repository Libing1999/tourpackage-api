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

@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Setting {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "key", nullable = false, length = 150)
    private String key;

    @Column(name = "value")
    private String value;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "is_public", nullable = false)
    private boolean visible;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
