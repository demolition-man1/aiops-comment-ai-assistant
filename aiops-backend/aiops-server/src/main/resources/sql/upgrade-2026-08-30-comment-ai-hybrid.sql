create table if not exists biz_comment_ai_decision (
    id bigint primary key auto_increment,
    comment_id bigint not null,
    shadow_result_id bigint not null,
    accepted_problem_type varchar(64) not null,
    confidence decimal(6,4) not null,
    gate_version varchar(32) not null,
    active tinyint not null default 1,
    activated_by bigint null,
    activated_at datetime not null default current_timestamp,
    unique key uk_comment_ai_decision_comment (comment_id),
    index idx_comment_ai_decision_active (active)
);
