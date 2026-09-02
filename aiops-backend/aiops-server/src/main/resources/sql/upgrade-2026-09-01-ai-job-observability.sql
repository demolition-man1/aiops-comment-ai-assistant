alter table biz_ai_call_log add column if not exists job_id bigint null after id;
alter table biz_ai_call_log add column if not exists queue_latency_ms bigint null after latency_ms;
alter table biz_ai_call_log add column if not exists total_latency_ms bigint null after queue_latency_ms;
alter table biz_ai_call_log add column if not exists error_code varchar(64) null after total_latency_ms;
create index if not exists idx_ai_call_log_job on biz_ai_call_log (job_id);
