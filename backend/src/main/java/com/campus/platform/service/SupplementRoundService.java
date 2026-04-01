package com.campus.platform.service;

import com.campus.platform.entity.SupplementRound;
import com.campus.platform.repository.SupplementRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * 补录轮次周期服务
 * 判断当前是否在补录周期内
 */
@Service
@RequiredArgsConstructor
public class SupplementRoundService {

    private final SupplementRoundRepository supplementRoundRepository;

    /**
     * 判断当前时间是否在补录周期内
     * 条件：存在状态为ACTIVE且当前时间在start_time~end_time之间的轮次
     */
    public boolean isWithinSupplementPeriod(Instant now) {
        return supplementRoundRepository.findActiveWithinPeriod(now).isPresent();
    }

    /**
     * 判断当前时间是否在补录周期内（使用系统当前时间）
     */
    public boolean isWithinSupplementPeriod() {
        return isWithinSupplementPeriod(Instant.now());
    }

    /**
     * 获取当前进行中的补录轮次（ACTIVE且在有效期内）
     */
    public Optional<SupplementRound> getCurrentActiveRound() {
        return supplementRoundRepository.findActiveWithinPeriod(Instant.now());
    }

    /**
     * 是否已有进行中的补录轮次（仅检查ACTIVE状态，不检查时间）
     */
    public boolean hasActiveRound() {
        return supplementRoundRepository.findActive().isPresent();
    }
}
