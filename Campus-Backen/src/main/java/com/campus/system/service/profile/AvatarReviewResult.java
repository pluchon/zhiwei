package com.campus.system.service.profile;

import lombok.Getter;

@Getter
public class AvatarReviewResult {

    private final boolean approved;
    private final String reason;

    private AvatarReviewResult(boolean approved, String reason) {
        this.approved = approved;
        this.reason = reason;
    }

    public static AvatarReviewResult approved() {
        return new AvatarReviewResult(true, "审核通过");
    }

    public static AvatarReviewResult rejected(String reason) {
        return new AvatarReviewResult(false, reason);
    }
}
