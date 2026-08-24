package com.aiops.constant;

public final class RedisKeyConstant {

    public static final String LOGIN_TOKEN = "login:token:%s";
    public static final String UPLOAD_CSV = "upload:csv:%s";
    public static final String TASK_STATUS = "task:status:%s";
    public static final String TASK_PROGRESS = "task:progress:%s";
    public static final String TASK_TYPE = "task:type:%s";
    public static final String ANALYSIS_PRODUCT = "analysis:product:%s";
    public static final String ANALYSIS_SELLER = "analysis:seller:%s";
    public static final String AI_REPORT = "ai:report:%s:%s:%s";
    public static final String AI_CONTENT = "ai:content:%s";
    public static final String AI_PRODUCT_COMPARE = "ai:compare:product:%s:%s";
    public static final String AI_RATE_LIMIT = "rate:ai:user:%s";
    public static final String HOT_PRODUCT_KEYWORDS = "hot:keywords:product:%s";

    private RedisKeyConstant() {
    }
}
