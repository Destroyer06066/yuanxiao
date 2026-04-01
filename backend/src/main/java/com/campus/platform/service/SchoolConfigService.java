package com.campus.platform.service;

import com.campus.platform.entity.SchoolConfig;
import com.campus.platform.repository.SchoolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchoolConfigService {

    private final SchoolConfigRepository schoolConfigRepository;

    private static final String SUPPLEMENT_MODE = "supplement_mode";
    private static final String INVITATION_DEFAULT_DAYS = "invitation_default_days";

    /**
     * 获取全局配置值（school_id = NULL）
     */
    public String getGlobalConfig(String configKey) {
        return schoolConfigRepository.findGlobalConfig(configKey)
                .map(SchoolConfig::getConfigValue)
                .orElse(null);
    }

    /**
     * 获取全局配置值，不存在则返回默认值
     */
    public String getGlobalConfig(String configKey, String defaultValue) {
        return schoolConfigRepository.findGlobalConfig(configKey)
                .map(SchoolConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * 设置全局配置值
     */
    public void setGlobalConfig(String configKey, String configValue) {
        Optional<SchoolConfig> existing = schoolConfigRepository.findGlobalConfig(configKey);
        if (existing.isPresent()) {
            SchoolConfig config = existing.get();
            config.setConfigValue(configValue);
            schoolConfigRepository.updateById(config);
        } else {
            SchoolConfig config = new SchoolConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            schoolConfigRepository.insert(config);
        }
    }

    /**
     * 获取所有全局配置
     */
    public List<SchoolConfig> getAllGlobalConfigs() {
        return schoolConfigRepository.findAllGlobalConfigs();
    }

    /**
     * 判断是否启用模式一（默认模式）
     */
    public boolean isSupplementModeOne() {
        String mode = getGlobalConfig(SUPPLEMENT_MODE);
        return "MODE_1".equals(mode) || mode == null;
    }

    /**
     * 判断是否启用模式二
     */
    public boolean isSupplementModeTwo() {
        return "MODE_2".equals(getGlobalConfig(SUPPLEMENT_MODE));
    }

    /**
     * 获取邀请默认有效期（天）
     */
    public int getInvitationDefaultDays() {
        String days = getGlobalConfig(INVITATION_DEFAULT_DAYS, "7");
        try {
            return Integer.parseInt(days);
        } catch (NumberFormatException e) {
            return 7;
        }
    }
}
