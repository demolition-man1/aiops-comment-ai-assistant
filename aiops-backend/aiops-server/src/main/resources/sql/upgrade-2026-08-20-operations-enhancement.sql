use aiops;

drop procedure if exists add_column_if_missing;

delimiter $$
create procedure add_column_if_missing(
    in table_name_value varchar(64),
    in column_name_value varchar(64),
    in column_definition_value text
)
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = table_name_value
          and column_name = column_name_value
    ) then
        set @alter_sql = concat(
            'alter table ',
            table_name_value,
            ' add column ',
            column_name_value,
            ' ',
            column_definition_value
        );
        prepare stmt from @alter_sql;
        execute stmt;
        deallocate prepare stmt;
    end if;
end$$
delimiter ;

call add_column_if_missing('biz_comment', 'manual_problem_type', 'varchar(64) null after problem_type');
call add_column_if_missing('biz_comment', 'custom_tags', 'json null after manual_problem_type');
call add_column_if_missing('biz_comment', 'tag_update_time', 'datetime null after is_negative');

call add_column_if_missing('biz_comment_analysis_result', 'custom_tag_distribution', 'json null after score_distribution');
call add_column_if_missing('biz_comment_analysis_result', 'trend_distribution', 'json null after custom_tag_distribution');

call add_column_if_missing('biz_negative_reply', 'effect_tag', 'varchar(64) null after model_name');
call add_column_if_missing('biz_negative_reply', 'use_count', 'int not null default 0 after effect_tag');
call add_column_if_missing('biz_negative_reply', 'favorite_flag', 'tinyint not null default 0 after use_count');
call add_column_if_missing('biz_negative_reply', 'update_time', 'datetime null after create_time');

drop procedure if exists add_column_if_missing;

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
