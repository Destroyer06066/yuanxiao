package com.campus.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

public interface NotificationRepository extends BaseMapper<Notification> {

    IPage<Notification> findByRecipientId(Page<Notification> page, @Param("recipientId") UUID recipientId);
}
