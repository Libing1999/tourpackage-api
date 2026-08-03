package com.tourpackage.api.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.entity.Setting;
import com.tourpackage.api.repository.SettingRepository;

@Service
@Transactional(readOnly = true)
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * Flattened key-value map of only the settings marked public — used to
     * drive the footer's contact info and social links. Anything not
     * flagged {@code is_public} (e.g. SMTP credentials, payment keys) never
     * reaches this method at all.
     */
    public Map<String, String> getPublicSettings() {
        return settingRepository.findByVisibleTrue().stream()
                .collect(Collectors.toMap(Setting::getKey, s -> s.getValue() == null ? "" : s.getValue()));
    }

}
