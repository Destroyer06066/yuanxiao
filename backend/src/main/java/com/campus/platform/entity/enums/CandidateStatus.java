package com.campus.platform.entity.enums;

public enum CandidateStatus {
    PENDING("待处理"),
    CONDITIONAL("有条件录取中"),
    ADMITTED("已录取（待确认）"),
    CONFIRMED("已确认"),
    MATERIAL_RECEIVED("材料已收"),
    CHECKED_IN("已报到"),
    REJECTED("已拒绝"),
    INVALIDATED("录取已失效");

    private final String description;

    CandidateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == CHECKED_IN || this == REJECTED || this == INVALIDATED;
    }

    public boolean isOperable() {
        return this == PENDING || this == CONDITIONAL || this == ADMITTED;
    }
}
