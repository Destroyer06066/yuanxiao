package com.campus.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.platform.common.result.Result;
import com.campus.platform.entity.CandidatePush;
import com.campus.platform.security.AccountPrincipal;
import com.campus.platform.security.SecurityContext;
import com.campus.platform.service.AdmissionService;
import com.campus.platform.service.CandidateService;
import com.campus.platform.service.OperationLogService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentController 单元测试")
class StudentControllerTest {

    @Mock private CandidateService candidateService;
    @Mock private AdmissionService admissionService;
    @Mock private OperationLogService operationLogService;

    @InjectMocks
    private StudentController studentController;

    private AccountPrincipal schoolAdmin;
    private UUID schoolId;

    @BeforeEach
    void setUp() {
        schoolId = UUID.randomUUID();
        schoolAdmin = new AccountPrincipal(
                UUID.randomUUID(), "SCHOOL_ADMIN", schoolId, "院校管理员", "jti-sa", List.of());
        SecurityContext.set(schoolAdmin);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    // ========== GET /api/v1/students ==========

    @Test
    @DisplayName("查询考生列表 - 返回分页数据")
    void list_returnsPagedResults() {
        Page<CandidatePush> page = new Page<>(1, 20);
        CandidatePush candidate = new CandidatePush();
        candidate.setPushId(UUID.randomUUID());
        candidate.setCandidateName("张三");
        page.setRecords(List.of(candidate));
        page.setTotal(1);

        when(candidateService.queryCandidates(
                eq(schoolId), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                eq("pushedAt"), eq("DESC"), eq(1), eq(20)))
                .thenReturn(page);

        Result<Map<String, Object>> result = studentController.list(
                null, null, null, null, null,
                null, null, null, null,
                "pushedAt", "DESC", 1, 20);

        assertThat(result.getCode()).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<CandidatePush> records = (List<CandidatePush>) result.getData().get("records");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCandidateName()).isEqualTo("张三");
        assertThat(result.getData().get("total")).isEqualTo(1L);
    }

    // ========== GET /api/v1/students/{pushId} ==========

    @Test
    @DisplayName("查询考生详情 - 存在返回详情")
    void detail_found_returnsCandidate() {
        UUID pushId = UUID.randomUUID();
        CandidatePush candidate = new CandidatePush();
        candidate.setPushId(pushId);
        candidate.setCandidateName("李四");

        when(candidateService.getById(pushId)).thenReturn(Optional.of(candidate));

        Result<CandidatePush> result = studentController.detail(pushId);

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData().getCandidateName()).isEqualTo("李四");
    }

    @Test
    @DisplayName("查询考生详情 - 不存在返回错误码")
    void detail_notFound_returnsError() {
        UUID pushId = UUID.randomUUID();
        when(candidateService.getById(pushId)).thenReturn(Optional.empty());

        Result<CandidatePush> result = studentController.detail(pushId);

        assertThat(result.getCode()).isEqualTo(30001);
        assertThat(result.getMessage()).isEqualTo("考生记录不存在");
    }

    // ========== POST /api/v1/students/admit ==========

    @Test
    @DisplayName("直接录取 - 成功")
    void admit_success() {
        UUID pushId = UUID.randomUUID();
        UUID majorId = UUID.randomUUID();

        StudentController.AdmitRequest req = new StudentController.AdmitRequest();
        req.setPushId(pushId);
        req.setMajorId(majorId);
        req.setRemark("优秀");

        doNothing().when(admissionService).directAdmission(pushId, majorId, "优秀", schoolAdmin.getAccountId());

        Result<Void> result = studentController.admit(req);

        assertThat(result.getCode()).isEqualTo(0);
        verify(admissionService).directAdmission(pushId, majorId, "优秀", schoolAdmin.getAccountId());
    }

    // ========== POST /api/v1/students/batch-reject ==========

    @Test
    @DisplayName("批量拒绝 - 成功")
    void batchReject_success() {
        UUID pushId1 = UUID.randomUUID();
        UUID pushId2 = UUID.randomUUID();

        StudentController.BatchRejectRequest req = new StudentController.BatchRejectRequest();
        req.setPushIds(List.of(pushId1, pushId2));

        Result<Void> result = studentController.batchReject(req);

        assertThat(result.getCode()).isEqualTo(0);
        verify(admissionService).rejectAdmission(pushId1, null, schoolAdmin.getAccountId());
        verify(admissionService).rejectAdmission(pushId2, null, schoolAdmin.getAccountId());
    }

    // ========== GET /api/v1/students/search ==========

    @Test
    @DisplayName("搜索考生 - SCHOOL_ADMIN 只查本校")
    void search_schoolAdmin_searchesOwnSchool() {
        CandidatePush candidate = new CandidatePush();
        candidate.setCandidateName("王五");

        when(candidateService.searchCandidates(eq(schoolId), eq("王五")))
                .thenReturn(List.of(candidate));

        Result<List<CandidatePush>> result = studentController.search("王五");

        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getCandidateName()).isEqualTo("王五");
        verify(candidateService).searchCandidates(schoolId, "王五");
    }

    @Test
    @DisplayName("搜索考生 - OP_ADMIN 查全部")
    void search_opAdmin_searchesAll() {
        AccountPrincipal opAdmin = new AccountPrincipal(
                UUID.randomUUID(), "OP_ADMIN", null, "运营管理员", "jti-op", List.of("*"));
        SecurityContext.set(opAdmin);

        when(candidateService.searchCandidates(isNull(), eq("张")))
                .thenReturn(List.of());

        Result<List<CandidatePush>> result = studentController.search("张");

        assertThat(result.getCode()).isEqualTo(0);
        verify(candidateService).searchCandidates(null, "张");
    }
}
