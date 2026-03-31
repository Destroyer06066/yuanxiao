package com.campus.mock.data;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 演示用内存数据存储
 * 模拟报名平台的考生数据和通知数据
 */
@Component
public class MockDataStore {

    // ==================== 模拟院校数据 ====================

    public static final List<MockSchool> SCHOOLS = List.of(
            new MockSchool("550e8400-e29b-41d4-a716-446655440001", "清华大学", "北京", "综合类"),
            new MockSchool("550e8400-e29b-41d4-a716-446655440002", "北京大学", "北京", "综合类"),
            new MockSchool("550e8400-e29b-41d4-a716-446655440003", "复旦大学", "上海", "综合类")
    );

    // ==================== 模拟考生账号 ====================

    public static final List<MockCandidate> DEMO_CANDIDATES = List.of(
            new MockCandidate(
                    "CAND-001", "李明", "Li Ming",
                    "campus001@example.com", "CN", "PASSPORT", "E12345678",
                    new BigDecimal("285"),
                    Map.of("数学", new BigDecimal("92"), "物理", new BigDecimal("88"), "化学", new BigDecimal("85"), "语文", new BigDecimal("80")),
                    "计算机科学与技术",
                    Instant.parse("2026-03-10T09:30:00Z"), 1
            ),
            new MockCandidate(
                    "CAND-002", "王芳", "Wang Fang",
                    "campus002@example.com", "US", "PASSPORT", "P9876543",
                    new BigDecimal("268"),
                    Map.of("数学", new BigDecimal("85"), "物理", new BigDecimal("82"), "化学", new BigDecimal("78"), "语文", new BigDecimal("88")),
                    "国际关系",
                    Instant.parse("2026-03-12T14:20:00Z"), 1
            ),
            new MockCandidate(
                    "CAND-003", "阿里·阿布杜拉", "Ali Abdullah",
                    "campus003@example.com", "PK", "PASSPORT", "A4567890",
                    new BigDecimal("245"),
                    Map.of("数学", new BigDecimal("75"), "物理", new BigDecimal("70"), "化学", new BigDecimal("65"), "语文", new BigDecimal("90")),
                    "阿拉伯语语言文学",
                    Instant.parse("2026-03-15T11:00:00Z"), 1
            )
    );

    // ==================== 运行时数据（通知 + 推送记录） ====================

    /** key: candidateId, value: list of push records */
    private final Map<String, List<PushRecord>> pushRecords = new ConcurrentHashMap<>();

    /** key: candidateId, value: sorted list of notifications (newest first) */
    private final Map<String, List<MockNotification>> notifications = new ConcurrentHashMap<>();

    /** key: candidateId */
    private final Map<String, MockCandidate> candidateMap = new ConcurrentHashMap<>();

    public MockDataStore() {
        initCandidateMap();
        initPushRecords();
        initNotifications();
    }

    private void initCandidateMap() {
        for (MockCandidate c : DEMO_CANDIDATES) {
            candidateMap.put(c.getCandidateId(), c);
        }
    }

    private void initPushRecords() {
        // CAND-001 推送到清华（待审核）、北大（已录取）
        pushRecords.put("CAND-001", new ArrayList<>(List.of(
                new PushRecord("PUSH-001", "CAND-001", "李明",
                        "550e8400-e29b-41d4-a716-446655440001", "清华大学",
                        "计算机科学与技术", "PENDING",
                        Instant.parse("2026-03-10T09:30:00Z"), 1, null, null),
                new PushRecord("PUSH-002", "CAND-001", "李明",
                        "550e8400-e29b-41d4-a716-446655440002", "北京大学",
                        "计算机科学与技术", "ADMITTED",
                        Instant.parse("2026-03-10T09:30:00Z"), 1,
                        "计算机科学与技术", null)
        )));

        // CAND-002 推送到复旦（条件录取）、清华（已拒绝）
        pushRecords.put("CAND-002", new ArrayList<>(List.of(
                new PushRecord("PUSH-003", "CAND-002", "王芳",
                        "550e8400-e29b-41d4-a716-446655440003", "复旦大学",
                        "国际关系", "CONDITIONAL",
                        Instant.parse("2026-03-12T14:20:00Z"), 1,
                        "国际关系", "需补充语言水平证明（HSK5级及以上），截止2026-04-10"),
                new PushRecord("PUSH-004", "CAND-002", "王芳",
                        "550e8400-e29b-41d4-a716-446655440001", "清华大学",
                        "国际关系", "REJECTED",
                        Instant.parse("2026-03-12T14:20:00Z"), 1, null, null)
        )));

        // CAND-003 推送到北大（待审核）
        pushRecords.put("CAND-003", new ArrayList<>(List.of(
                new PushRecord("PUSH-005", "CAND-003", "阿里·阿布杜拉",
                        "550e8400-e29b-41d4-a716-446655440002", "北京大学",
                        "阿拉伯语语言文学", "PENDING",
                        Instant.parse("2026-03-15T11:00:00Z"), 1, null, null)
        )));
    }

    private void initNotifications() {
        notifications.put("CAND-001", new ArrayList<>(List.of(
                createNotification("CAND-001", "NOTIF-001", "ADMISSION",
                        "录取通知", "您已被北京大学正式录取！录取专业：计算机科学与技术。请在收到通知后尽快确认。",
                        Instant.parse("2026-03-20T10:00:00Z"), false),
                createNotification("CAND-001", "NOTIF-002", "SYSTEM",
                        "成绩已推送", "您的成绩已成功推送到清华大学和北京大学，请等待院校审核。",
                        Instant.parse("2026-03-10T09:35:00Z"), true)
        )));

        notifications.put("CAND-002", new ArrayList<>(List.of(
                createNotification("CAND-002", "NOTIF-003", "CONDITIONAL",
                        "有条件录取通知", "您已被复旦大学有条件录取！\n录取专业：国际关系\n条件：需补充语言水平证明（HSK5级及以上）\n截止日期：2026-04-10\n请尽快完成条件后联系院校确认。",
                        Instant.parse("2026-03-18T10:00:00Z"), false),
                createNotification("CAND-002", "NOTIF-004", "REJECTION",
                        "未录取通知", "很遗憾，清华大学对您的申请暂未通过，请关注其他院校通知。",
                        Instant.parse("2026-03-16T15:00:00Z"), false)
        )));

        notifications.put("CAND-003", new ArrayList<>(List.of(
                createNotification("CAND-003", "NOTIF-005", "SYSTEM",
                        "成绩已推送", "您的成绩已成功推送到北京大学，请等待院校审核。",
                        Instant.parse("2026-03-15T11:05:00Z"), true)
        )));
    }

    private MockNotification createNotification(String candidateId, String id, String type,
                                               String title, String content, Instant createdAt, boolean read) {
        return new MockNotification(id, candidateId, type, title, content, createdAt.toString(), read);
    }

    // ==================== 公开 API ====================

    public MockCandidate login(String candidateId) {
        MockCandidate c = candidateMap.get(candidateId);
        if (c == null) throw new RuntimeException("考生编号不存在");
        return c;
    }

    public List<PushRecord> getPushRecords(String candidateId) {
        return pushRecords.getOrDefault(candidateId, Collections.emptyList());
    }

    public List<MockNotification> getNotifications(String candidateId) {
        List<MockNotification> list = notifications.getOrDefault(candidateId, Collections.emptyList());
        return list.stream()
                .sorted(Comparator.comparing(MockNotification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public int getUnreadCount(String candidateId) {
        return (int) getNotifications(candidateId).stream().filter(n -> !n.isRead()).count();
    }

    public void markNotificationRead(String candidateId, String notificationId) {
        notifications.computeIfAbsent(candidateId, k -> new ArrayList<>())
                .stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(MockNotification::setReadTrue);
    }

    public void markAllNotificationsRead(String candidateId) {
        notifications.computeIfAbsent(candidateId, k -> new ArrayList<>())
                .forEach(MockNotification::setReadTrue);
    }

    /**
     * 考生确认录取
     * @return 确认的学校信息，以及被失效的学校列表
     */
    public ConfirmResult confirmAdmission(String candidateId, String schoolId) {
        List<PushRecord> records = pushRecords.get(candidateId);
        if (records == null) throw new RuntimeException("无推送记录");

        PushRecord toConfirm = records.stream()
                .filter(r -> r.getSchoolId().equals(schoolId) && "ADMITTED".equals(r.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("该院校无有效录取通知可确认"));

        toConfirm.setStatus("CONFIRMED");
        toConfirm.setConfirmedAt(Instant.now().toString());

        // 找出其他 ADMITTED → INVALIDATED
        List<PushRecord> invalidated = records.stream()
                .filter(r -> "ADMITTED".equals(r.getStatus()) && !r.getSchoolId().equals(schoolId))
                .peek(r -> {
                    r.setStatus("INVALIDATED");
                    r.setInvalidatedReason("您已在其他院校确认录取，本录取通知已自动失效");
                })
                .collect(Collectors.toList());

        // 为失效学校添加通知
        for (PushRecord r : invalidated) {
            MockSchool school = SCHOOLS.stream()
                    .filter(s -> s.getId().equals(r.getSchoolId())).findFirst().orElse(null);
            String schoolName = school != null ? school.getName() : "该院校";
            addNotification(candidateId, new MockNotification(
                    "NOTIF-" + System.currentTimeMillis(),
                    candidateId,
                    "INVALIDATION",
                    "录取通知已失效",
                    "您已在其他院校确认录取，" + schoolName + "的录取通知已自动失效，名额已释放。",
                    Instant.now().toString(),
                    false
            ));
        }

        // 添加确认成功通知
        MockSchool confirmedSchool = SCHOOLS.stream()
                .filter(s -> s.getId().equals(schoolId)).findFirst().orElse(null);
        addNotification(candidateId, new MockNotification(
                "NOTIF-" + System.currentTimeMillis(),
                candidateId,
                "CONFIRMED",
                "录取已确认",
                "您已确认接受" + (confirmedSchool != null ? confirmedSchool.getName() : "") + "的录取！请按院校要求准备并寄送入学材料。",
                Instant.now().toString(),
                false
        ));

        return new ConfirmResult(confirmedSchool, invalidated);
    }

    public void giveUpAdmission(String candidateId, String schoolId) {
        List<PushRecord> records = pushRecords.get(candidateId);
        if (records == null) return;

        records.stream()
                .filter(r -> r.getSchoolId().equals(schoolId) && "ADMITTED".equals(r.getStatus()))
                .findFirst()
                .ifPresent(r -> r.setStatus("PENDING"));

        MockSchool school = SCHOOLS.stream()
                .filter(s -> s.getId().equals(schoolId)).findFirst().orElse(null);
        addNotification(candidateId, new MockNotification(
                "NOTIF-" + System.currentTimeMillis(),
                candidateId,
                "GAVE_UP",
                "录取已放弃",
                "您已放弃" + (school != null ? school.getName() : "") + "的录取，该院校可将名额分配给其他考生。",
                Instant.now().toString(),
                false
        ));
    }

    public void confirmMaterialSent(String candidateId, String schoolId, String trackingNo, String remark) {
        List<PushRecord> records = pushRecords.get(candidateId);
        if (records == null) return;

        records.stream()
                .filter(r -> r.getSchoolId().equals(schoolId) && "CONFIRMED".equals(r.getStatus()))
                .findFirst()
                .ifPresent(r -> r.setStatus("MATERIAL_SENT"));

        MockSchool school = SCHOOLS.stream()
                .filter(s -> s.getId().equals(schoolId)).findFirst().orElse(null);
        addNotification(candidateId, new MockNotification(
                "NOTIF-" + System.currentTimeMillis(),
                candidateId,
                "MATERIAL_SENT",
                "材料已寄送",
                "您已确认寄送入学材料至" + (school != null ? school.getName() : "") + "。"
                        + (trackingNo != null && !trackingNo.isBlank() ? "快递单号：" + trackingNo : ""),
                Instant.now().toString(),
                false
        ));
    }

    // ==================== 内部数据操作（供 MockController 调用） ====================

    public void addNotification(String candidateId, MockNotification notification) {
        notifications.computeIfAbsent(candidateId, k -> new ArrayList<>()).add(0, notification);
    }

    public void updatePushStatus(String candidateId, String schoolId, String status, String remark) {
        List<PushRecord> records = pushRecords.get(candidateId);
        if (records == null) return;
        records.stream()
                .filter(r -> r.getSchoolId().equals(schoolId))
                .findFirst()
                .ifPresent(r -> {
                    r.setStatus(status);
                    if (remark != null) r.setRemark(remark);
                });
    }

    // ==================== 数据类（纯 POJO，无 Lombok） ====================

    public static class MockSchool {
        private final String id;
        private final String name;
        private final String city;
        private final String type;

        public MockSchool(String id, String name, String city, String type) {
            this.id = id; this.name = name; this.city = city; this.type = type;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCity() { return city; }
        public String getType() { return type; }
    }

    public static class MockCandidate {
        private final String candidateId;
        private final String name;
        private final String englishName;
        private final String email;
        private final String nationality;
        private final String idType;
        private final String idNumber;
        private final BigDecimal totalScore;
        private final Map<String, BigDecimal> subjectScores;
        private final String intention;
        private final Instant pushedAt;
        private final Integer round;

        public MockCandidate(String candidateId, String name, String englishName, String email,
                             String nationality, String idType, String idNumber,
                             BigDecimal totalScore, Map<String, BigDecimal> subjectScores,
                             String intention, Instant pushedAt, Integer round) {
            this.candidateId = candidateId; this.name = name; this.englishName = englishName;
            this.email = email; this.nationality = nationality; this.idType = idType;
            this.idNumber = idNumber; this.totalScore = totalScore;
            this.subjectScores = subjectScores; this.intention = intention;
            this.pushedAt = pushedAt; this.round = round;
        }
        public String getCandidateId() { return candidateId; }
        public String getName() { return name; }
        public String getEnglishName() { return englishName; }
        public String getEmail() { return email; }
        public String getNationality() { return nationality; }
        public String getIdType() { return idType; }
        public String getIdNumber() { return idNumber; }
        public BigDecimal getTotalScore() { return totalScore; }
        public Map<String, BigDecimal> getSubjectScores() { return subjectScores; }
        public String getIntention() { return intention; }
        public Instant getPushedAt() { return pushedAt; }
        public Integer getRound() { return round; }
    }

    public static class PushRecord {
        private String pushId;
        private String candidateId;
        private String candidateName;
        private String schoolId;
        private String schoolName;
        private String majorName;
        private String status;
        private Instant pushedAt;
        private Integer round;
        private String admissionMajor;
        private String remark;
        private String confirmedAt;
        private String invalidatedReason;

        public PushRecord() {}
        public PushRecord(String pushId, String candidateId, String candidateName,
                          String schoolId, String schoolName, String majorName, String status,
                          Instant pushedAt, Integer round, String admissionMajor, String remark) {
            this.pushId = pushId; this.candidateId = candidateId; this.candidateName = candidateName;
            this.schoolId = schoolId; this.schoolName = schoolName; this.majorName = majorName;
            this.status = status; this.pushedAt = pushedAt; this.round = round;
            this.admissionMajor = admissionMajor; this.remark = remark;
        }

        public String getPushId() { return pushId; }
        public String getCandidateId() { return candidateId; }
        public String getCandidateName() { return candidateName; }
        public String getSchoolId() { return schoolId; }
        public String getSchoolName() { return schoolName; }
        public String getMajorName() { return majorName; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public Instant getPushedAt() { return pushedAt; }
        public Integer getRound() { return round; }
        public String getAdmissionMajor() { return admissionMajor; }
        public String getRemark() { return remark; }
        public void setRemark(String r) { this.remark = r; }
        public String getConfirmedAt() { return confirmedAt; }
        public void setConfirmedAt(String t) { this.confirmedAt = t; }
        public String getInvalidatedReason() { return invalidatedReason; }
        public void setInvalidatedReason(String r) { this.invalidatedReason = r; }

        public String getStatusText() {
            return switch (status) {
                case "PENDING" -> "待审核";
                case "CONDITIONAL" -> "有条件录取";
                case "ADMITTED" -> "已录取（待确认）";
                case "CONFIRMED" -> "已确认";
                case "REJECTED" -> "未录取";
                case "INVALIDATED" -> "录取已失效";
                case "MATERIAL_SENT" -> "材料已寄送";
                default -> status;
            };
        }
    }

    public static class MockNotification {
        private final String id;
        private final String candidateId;
        private final String type;
        private final String title;
        private final String content;
        private final String createdAt;
        private boolean read;

        public MockNotification(String id, String candidateId, String type, String title,
                                String content, String createdAt, boolean read) {
            this.id = id; this.candidateId = candidateId; this.type = type;
            this.title = title; this.content = content; this.createdAt = createdAt; this.read = read;
        }
        public String getId() { return id; }
        public String getCandidateId() { return candidateId; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getCreatedAt() { return createdAt; }
        public boolean isRead() { return read; }
        public void setReadTrue() { this.read = true; }

        public String getTypeIcon() {
            return switch (type) {
                case "ADMISSION", "CONFIRMED" -> "✅";
                case "CONDITIONAL" -> "⏳";
                case "REJECTION", "INVALIDATION" -> "❌";
                case "MATERIAL_SENT" -> "📦";
                default -> "📢";
            };
        }

        public String getTypeClass() {
            return switch (type) {
                case "ADMISSION", "CONFIRMED" -> "type-success";
                case "CONDITIONAL" -> "type-warning";
                case "REJECTION", "INVALIDATION" -> "type-danger";
                case "MATERIAL_SENT" -> "type-info";
                default -> "type-default";
            };
        }
    }

    public static class ConfirmResult {
        private final MockSchool confirmedSchool;
        private final List<PushRecord> invalidatedSchools;

        public ConfirmResult(MockSchool confirmedSchool, List<PushRecord> invalidatedSchools) {
            this.confirmedSchool = confirmedSchool;
            this.invalidatedSchools = invalidatedSchools;
        }
        public MockSchool getConfirmedSchool() { return confirmedSchool; }
        public List<PushRecord> getInvalidatedSchools() { return invalidatedSchools; }
    }
}
