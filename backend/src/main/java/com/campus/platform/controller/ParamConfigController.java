package com.campus.platform.controller;

import com.campus.platform.common.result.Result;
import com.campus.platform.entity.SchoolConfig;
import com.campus.platform.service.SchoolConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/params")
@RequiredArgsConstructor
public class ParamConfigController {

    private final SchoolConfigService schoolConfigService;

    /**
     * 获取所有全局配置
     */
    @GetMapping("/global")
    public Result<List<SchoolConfig>> getGlobalConfigs() {
        return Result.ok(schoolConfigService.getAllGlobalConfigs());
    }

    /**
     * 更新全局配置
     */
    @PutMapping("/global/{configKey}")
    public Result<Void> updateGlobalConfig(@PathVariable String configKey, @RequestBody UpdateConfigRequest request) {
        schoolConfigService.setGlobalConfig(configKey, request.configValue());
        return Result.ok();
    }

    public record UpdateConfigRequest(String configValue) {}
}
