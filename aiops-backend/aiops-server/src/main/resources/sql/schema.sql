create table if not exists sys_user (
    id bigint primary key auto_increment,
    username varchar(64) not null unique,
    password varchar(255) not null,
    nickname varchar(64) null,
    email varchar(128) null,
    role varchar(32) not null default 'merchant',
    status tinyint not null default 1,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp
);

create table if not exists sys_file_upload (
    id bigint primary key auto_increment,
    user_id bigint null,
    original_name varchar(255) not null,
    file_name varchar(255) not null,
    object_key varchar(512) not null,
    file_url varchar(1024) null,
    file_type varchar(64) null,
    file_size bigint null,
    business_type varchar(64) null,
    status tinyint not null default 1,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp
);

create table if not exists biz_product (
    id bigint primary key auto_increment,
    product_id varchar(64) not null unique,
    seller_id varchar(64) null,
    category_name varchar(128) null,
    category_name_en varchar(128) null,
    avg_price decimal(10,2) null,
    avg_freight decimal(10,2) null,
    order_count int null,
    review_count int null,
    avg_score decimal(4,2) null,
    negative_rate decimal(6,4) null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_product_seller_id (seller_id),
    index idx_product_category (category_name_en)
);

create table if not exists biz_seller (
    id bigint primary key auto_increment,
    seller_id varchar(64) not null unique,
    seller_city varchar(128) null,
    seller_state varchar(32) null,
    product_count int null,
    order_count int null,
    avg_score decimal(4,2) null,
    negative_rate decimal(6,4) null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp
);

create table if not exists biz_comment (
    id bigint primary key auto_increment,
    review_id varchar(64) null,
    order_id varchar(64) null,
    product_id varchar(64) null,
    seller_id varchar(64) null,
    review_score int null,
    review_title text null,
    review_content text null,
    clean_content text null,
    review_time datetime null,
    sentiment varchar(32) null,
    sentiment_score decimal(6,4) null,
    keywords json null,
    problem_type varchar(64) null,
    manual_problem_type varchar(64) null,
    custom_tags json null,
    is_negative tinyint null,
    tag_update_time datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_comment_product_id (product_id),
    index idx_comment_seller_id (seller_id),
    index idx_comment_sentiment (sentiment),
    index idx_comment_score (review_score),
    index idx_comment_problem_type (problem_type)
);

create table if not exists biz_custom_tag (
    id bigint primary key auto_increment,
    tag_name varchar(64) not null,
    tag_group varchar(64) null,
    color varchar(32) null,
    description varchar(255) null,
    sort_order int not null default 0,
    enabled tinyint not null default 1,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    unique key uk_custom_tag_name (tag_name),
    index idx_custom_tag_group (tag_group),
    index idx_custom_tag_enabled (enabled)
);

create table if not exists biz_problem_solution (
    id bigint primary key auto_increment,
    problem_type varchar(64) not null,
    category_name_en varchar(128) null,
    solution_title varchar(128) not null,
    solution_content text not null,
    keywords varchar(255) null,
    source_type varchar(64) null,
    priority int not null default 0,
    use_count int not null default 0,
    enabled tinyint not null default 1,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    unique key uk_problem_solution_title (problem_type, solution_title),
    index idx_problem_solution_type (problem_type),
    index idx_problem_solution_category (category_name_en),
    index idx_problem_solution_enabled (enabled)
);

create table if not exists sys_prompt_template (
    id bigint primary key auto_increment,
    template_name varchar(128) not null,
    business_type varchar(64) not null,
    language varchar(16) not null default 'zh-CN',
    template_content longtext not null,
    variable_schema json null,
    default_flag tinyint not null default 0,
    enabled tinyint not null default 1,
    remark varchar(512) null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    unique key uk_prompt_template_name (business_type, language, template_name),
    index idx_prompt_template_business (business_type, language),
    index idx_prompt_template_default (business_type, language, default_flag),
    index idx_prompt_template_enabled (enabled)
);

create table if not exists biz_ai_call_log (
    id bigint primary key auto_increment,
    user_id bigint null,
    business_type varchar(64) not null,
    target_type varchar(32) null,
    target_id varchar(128) null,
    prompt_template_id bigint null,
    model_name varchar(64) null,
    call_status varchar(32) not null,
    token_usage int null,
    estimated_cost decimal(12,6) null,
    latency_ms bigint null,
    error_message text null,
    create_time datetime not null default current_timestamp,
    index idx_ai_call_log_business (business_type),
    index idx_ai_call_log_status (call_status),
    index idx_ai_call_log_target (target_type, target_id),
    index idx_ai_call_log_create_time (create_time)
);

create table if not exists biz_analysis_task (
    id bigint primary key auto_increment,
    user_id bigint null,
    target_type varchar(32) null,
    target_id varchar(64) null,
    task_type varchar(64) not null,
    task_status varchar(32) not null,
    progress int not null default 0,
    request_param json null,
    error_message text null,
    start_time datetime null,
    end_time datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_analysis_task_target (target_type, target_id),
    index idx_analysis_task_status (task_status)
);

create table if not exists biz_crawl_task (
    id bigint primary key auto_increment,
    user_id bigint null,
    platform varchar(32) not null,
    target_url varchar(1024) not null,
    target_type varchar(64) null,
    task_status varchar(32) not null,
    progress int not null default 0,
    max_count int null,
    success_count int null,
    fail_count int null,
    delay_seconds int null,
    request_param json null,
    error_message text null,
    start_time datetime null,
    end_time datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_crawl_task_status (task_status)
);

create table if not exists biz_comment_analysis_result (
    id bigint primary key auto_increment,
    task_id bigint null,
    target_type varchar(32) not null,
    target_id varchar(64) not null,
    total_count int null,
    positive_count int null,
    neutral_count int null,
    negative_count int null,
    positive_rate decimal(6,4) null,
    negative_rate decimal(6,4) null,
    top_keywords json null,
    negative_keywords json null,
    problem_distribution json null,
    score_distribution json null,
    custom_tag_distribution json null,
    trend_distribution json null,
    summary text null,
    create_time datetime not null default current_timestamp,
    index idx_analysis_result_target (target_type, target_id),
    index idx_analysis_result_task_id (task_id)
);

create table if not exists biz_operation_report (
    id bigint primary key auto_increment,
    task_id bigint null,
    target_type varchar(32) not null,
    target_id varchar(64) not null,
    report_title varchar(255) null,
    consumer_pain_points text null,
    product_advantages text null,
    product_disadvantages text null,
    operation_suggestions text null,
    copywriting_suggestions text null,
    service_suggestions text null,
    risk_tips text null,
    full_report longtext null,
    model_name varchar(64) null,
    create_time datetime not null default current_timestamp,
    index idx_operation_report_target (target_type, target_id)
);

create table if not exists biz_ai_content_record (
    id bigint primary key auto_increment,
    user_id bigint null,
    target_type varchar(32) not null,
    target_id varchar(64) not null,
    content_type varchar(64) not null,
    style_type varchar(64) null,
    prompt text null,
    generated_content longtext null,
    model_name varchar(64) null,
    token_usage int null,
    create_time datetime not null default current_timestamp,
    index idx_ai_content_target (target_type, target_id),
    index idx_ai_content_type (content_type)
);

create table if not exists biz_negative_reply (
    id bigint primary key auto_increment,
    comment_id bigint not null,
    product_id varchar(64) null,
    seller_id varchar(64) null,
    problem_type varchar(64) null,
    comment_content text null,
    tone_type varchar(64) null,
    reply_content text null,
    model_name varchar(64) null,
    effect_tag varchar(64) null,
    use_count int not null default 0,
    favorite_flag tinyint not null default 0,
    create_time datetime not null default current_timestamp,
    update_time datetime null,
    index idx_negative_reply_comment_id (comment_id),
    index idx_negative_reply_product_id (product_id),
    index idx_negative_reply_seller_id (seller_id)
);

create table if not exists biz_product_compare_report (
    id bigint primary key auto_increment,
    left_product_id varchar(64) not null,
    right_product_id varchar(64) not null,
    metric_snapshot json null,
    compare_summary text null,
    advantage_analysis text null,
    risk_analysis text null,
    operation_suggestions text null,
    model_name varchar(64) null,
    create_time datetime not null default current_timestamp,
    index idx_product_compare_pair (left_product_id, right_product_id)
);

create table if not exists biz_sync_config (
    id bigint primary key auto_increment,
    user_id bigint null,
    sync_name varchar(128) not null,
    source_type varchar(32) not null,
    data_source varchar(64) null,
    import_mode varchar(32) not null default 'incremental',
    data_path varchar(1024) null,
    file_id bigint null,
    object_key varchar(512) null,
    file_url varchar(1024) null,
    platform varchar(32) null,
    target_url varchar(1024) null,
    target_type varchar(64) null,
    max_count int null,
    delay_seconds int null,
    cron_expression varchar(128) not null,
    auto_analysis tinyint not null default 0,
    enabled tinyint not null default 1,
    remark varchar(512) null,
    last_run_time datetime null,
    next_run_time datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_sync_config_enabled (enabled),
    index idx_sync_config_source_type (source_type)
);

create table if not exists biz_sync_execution (
    id bigint primary key auto_increment,
    config_id bigint not null,
    trigger_type varchar(32) not null,
    execution_status varchar(32) not null,
    linked_task_id bigint null,
    linked_task_type varchar(64) null,
    error_message text null,
    start_time datetime null,
    end_time datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_sync_execution_config (config_id),
    index idx_sync_execution_status (execution_status),
    index idx_sync_execution_create_time (create_time)
);

create table if not exists biz_task_record (
    id bigint primary key auto_increment,
    task_name varchar(128) not null,
    task_type varchar(64) not null,
    task_status varchar(32) not null,
    progress int not null default 0,
    source_table varchar(64) null,
    source_id bigint null,
    target_type varchar(32) null,
    target_id varchar(64) null,
    request_param json null,
    error_message text null,
    start_time datetime null,
    end_time datetime null,
    create_time datetime not null default current_timestamp,
    update_time datetime not null default current_timestamp on update current_timestamp,
    index idx_task_record_type_status (task_type, task_status),
    index idx_task_record_source (source_table, source_id),
    index idx_task_record_create_time (create_time)
);
