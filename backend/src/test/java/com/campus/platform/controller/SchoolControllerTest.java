package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.School;
import com.campus.platform.entity.enums.SchoolStatus;
import com.campus.platform.repository.SchoolRepository;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchoolController 单元测试")
class SchoolControllerTest {

    @Mock private SchoolRepository schoolRepository;

    @InjectMocks
    private SchoolController schoolController;

    private AccountPrincipal opAdmin;
    private UUID testSchoolId;

    @BeforeEach
    void setUp() {
        opAdmin = new AccountPrincipal(
                UUID.randomUUID(), "OP_ADMIN", null, "运营管理员", "jti-op", List.of("*"));
        SecurityContext.set(opAdmin);
        testSchoolId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    // ========== GET /api/v1/admin/schools ==========

    @Test
    @DisplayName("查询院校列表 - 返回分页列表")
    void list_returnsPagedResults() {
        Page<School> page = new Page<>(1, 20);
        School school = new School();
        school.setSchoolId(testSchoolId);
        school.setSchoolName("测试大学");
        page.setRecords(List.of(school));
        page.setTotal(1);

        when(schoolRepository.pageQuery(any(), isNull(), isNull(), isNull(), eq("ACTIVE")))
                .thenReturn(page);

        Result<Map<String, Object>> result = schoolController.list(null, null, null, "ACTIVE", 1, 20);

        assertThat(result.getCode()).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<School> records = (List<School>) result.getData().get("records");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getSchoolName()).isEqualTo("测试大学");
        assertThat(result.getData().get("total")).isEqualTo(1L);
    }

    // ========== POST /api/v1/admin/schools ==========

    @Test
    @DisplayName("创建院校 - 成功返回 schoolId")
    void create_success_returnsSchoolId() {
        when(schoolRepository.existsByName("新院校")).thenReturn(false);

        SchoolController.CreateSchoolRequest req = buildCreateRequest("新院校");

        Result<Map<String, Object>> result = schoolController.create(req);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).containsKey("schoolId");
    }

    @Test
    @DisplayName("创建院校 - 重复名称返回错误码")
    void create_duplicateName_returnsError() {
        when(schoolRepository.existsByName("已有院校")).thenReturn(true);

        SchoolController.CreateSchoolRequest req = buildCreateRequest("已有院校");

        Result<Map<String, Object>> result = schoolController.create(req);

        assertThat(result.getCode()).isEqualTo(20002);
        assertThat(result.getMessage()).isEqualTo("院校名称已存在");
    }

    // ========== PUT /api/v1/admin/schools/{id} ==========

    @Test
    @DisplayName("更新院校 - 成功")
    void update_success() {
        School existing = new School();
        existing.setSchoolId(testSchoolId);
        existing.setSchoolName("旧名称");
        when(schoolRepository.findById(testSchoolId)).thenReturn(Optional.of(existing));
        when(schoolRepository.existsByNameExcluding("新名称", testSchoolId)).thenReturn(false);
        when(schoolRepository.updateById(any(School.class))).thenReturn(1);

        SchoolController.UpdateSchoolRequest req = buildUpdateRequest("新名称");

        Result<Void> result = schoolController.update(testSchoolId, req);

        assertThat(result.getCode()).isEqualTo(0);
        verify(schoolRepository).updateById(any(School.class));
    }

    // ========== GET /api/v1/admin/schools/{id} ==========

    @Test
    @DisplayName("获取院校详情 - 成功")
    void getById_found_returnsSchool() {
        School school = new School();
        school.setSchoolId(testSchoolId);
        school.setSchoolName("测试大学");
        school.setStatus(SchoolStatus.ACTIVE.name());
        when(schoolRepository.findById(testSchoolId)).thenReturn(Optional.of(school));

        Result<School> result = schoolController.getById(testSchoolId);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getSchoolName()).isEqualTo("测试大学");
    }

    @Test
    @DisplayName("获取院校详情 - 不存在抛异常")
    void getById_notFound_throwsException() {
        when(schoolRepository.findById(testSchoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolController.getById(testSchoolId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("院校不存在");
    }

    // ========== 辅助方法 ==========

    private SchoolController.CreateSchoolRequest buildCreateRequest(String name) {
        SchoolController.CreateSchoolRequest req = new SchoolController.CreateSchoolRequest();
        req.setSchoolName(name);
        req.setSchoolShortName("测试");
        req.setProvince("北京");
        req.setSchoolType("本科");
        req.setContactName("张三");
        req.setContactPhone("13800138000");
        req.setContactEmail("test@example.com");
        return req;
    }

    private SchoolController.UpdateSchoolRequest buildUpdateRequest(String name) {
        SchoolController.UpdateSchoolRequest req = new SchoolController.UpdateSchoolRequest();
        req.setSchoolName(name);
        req.setSchoolShortName("测试");
        req.setProvince("北京");
        req.setSchoolType("本科");
        req.setContactName("张三");
        req.setContactPhone("13800138000");
        req.setContactEmail("test@example.com");
        return req;
    }
}
