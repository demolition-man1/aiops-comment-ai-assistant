create table if not exists biz_operation_report_evidence (
    id bigint primary key auto_increment,
    report_id bigint not null,
    source_type varchar(32) not null,
    source_id bigint not null,
    source_title varchar(255) null,
    relevance_score decimal(8,6) not null,
    retrieval_version varchar(64) not null,
    create_time datetime not null default current_timestamp,
    unique key uk_operation_report_evidence_source (report_id, source_type, source_id),
    index idx_operation_report_evidence_report (report_id),
    index idx_operation_report_evidence_source (source_type, source_id)
);
