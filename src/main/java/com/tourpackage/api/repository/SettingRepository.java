package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.Setting;

public interface SettingRepository extends JpaRepository<Setting, UUID> {

    List<Setting> findByVisibleTrue();

}
