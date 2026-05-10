package com.koinonia.backend.post;

public enum Topic {
    FAITH,
    PRAYER,
    WORSHIP,
    SCRIPTURE,
    COMMUNITY,
    TESTIMONY,
    ENCOURAGEMENT,
    DOCTRINE,
    GENERAL;

    public String getDisplayName() {
        return switch (this) {
            case FAITH        -> "Faith";
            case PRAYER       -> "Prayer";
            case WORSHIP      -> "Worship";
            case SCRIPTURE    -> "Scripture";
            case COMMUNITY    -> "Community";
            case TESTIMONY    -> "Testimony";
            case ENCOURAGEMENT -> "Encouragement";
            case DOCTRINE     -> "Doctrine";
            case GENERAL      -> "General";
        };
    }
}
