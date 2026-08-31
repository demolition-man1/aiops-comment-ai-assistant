set @schema_name = database();

set @rag_used_exists = (
    select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'biz_negative_reply' and column_name = 'rag_used'
);
set @rag_used_sql = if(
    @rag_used_exists = 0,
    'alter table biz_negative_reply add column rag_used tinyint not null default 0 after favorite_flag',
    'select 1'
);
prepare rag_used_statement from @rag_used_sql;
execute rag_used_statement;
deallocate prepare rag_used_statement;

set @rag_references_exists = (
    select count(*) from information_schema.columns
    where table_schema = @schema_name and table_name = 'biz_negative_reply' and column_name = 'rag_references'
);
set @rag_references_sql = if(
    @rag_references_exists = 0,
    'alter table biz_negative_reply add column rag_references json null after rag_used',
    'select 1'
);
prepare rag_references_statement from @rag_references_sql;
execute rag_references_statement;
deallocate prepare rag_references_statement;
