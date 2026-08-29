insert into sys_user
(id, username, password, nickname, email, role, status, create_time, update_time)
values
(1, 'admin', '$2a$10$JtOFioewneqMwmOJPoak8.lxg/a0sjIXl8seCyyLCUmeRYSngWq0y', 'admin', 'admin@example.com', 'admin', 1, now(), now())
on duplicate key update
password = values(password),
nickname = values(nickname),
email = values(email),
role = values(role),
status = values(status),
update_time = now();

insert into biz_custom_tag
(tag_name, tag_group, color, description, sort_order, enabled, create_time, update_time)
values
('包装破损', '物流体验', '#f97316', '用户反馈包装、防护、外箱相关问题', 90, 1, now(), now()),
('配送延迟', '物流体验', '#ef4444', '用户反馈送达时间、派送进度相关问题', 80, 1, now(), now()),
('尺寸不符', '商品体验', '#8b5cf6', '用户反馈尺寸、规格、适配度相关问题', 70, 1, now(), now()),
('做工瑕疵', '商品体验', '#0ea5e9', '用户反馈质量、材质、做工相关问题', 60, 1, now(), now())
on duplicate key update
tag_group = values(tag_group),
color = values(color),
description = values(description),
sort_order = values(sort_order),
enabled = values(enabled),
update_time = now();

insert into biz_problem_solution
(problem_type, category_name_en, solution_title, solution_content, keywords, source_type, priority, use_count, enabled, create_time, update_time)
values
('logistics', null, '优化物流异常跟进流程', '针对配送延迟和未收到货反馈，建立订单异常清单，优先联系物流商核实轨迹，并向顾客同步预计处理时间。', 'delivery,shipping,prazo,entrega,logistics', 'preset', 90, 0, 1, now(), now()),
('quality', null, '建立质量问题复盘清单', '将质量类差评按材质、做工、破损、缺件归档，汇总高频 SKU 与供应商批次，形成每周复盘项。', 'quality,qualidade,damage,defect', 'preset', 80, 0, 1, now(), now()),
('size', null, '完善规格与尺码说明', '在详情页强化尺寸、型号、适配范围和测量方式说明，降低用户预期偏差导致的退换货。', 'size,dimension,modelo,tamanho', 'preset', 70, 0, 1, now(), now()),
('price', null, '沉淀价格敏感用户补偿策略', '对价格波动或性价比反馈建立优惠券、组合装和会员权益方案，用于客服解释和复购引导。', 'price,preco,value,coupon', 'preset', 60, 0, 1, now(), now())
on duplicate key update
solution_content = values(solution_content),
keywords = values(keywords),
source_type = values(source_type),
priority = values(priority),
enabled = values(enabled),
update_time = now();

insert into sys_prompt_template
(template_name, business_type, language, template_content, variable_schema, default_flag, enabled, remark, create_time, update_time)
values
('默认运营报告模板', 'report', 'zh-CN',
'你是中小电商商家的AI运营顾问。请基于评论分析结果生成一份运营报告。必须只输出JSON，不要输出Markdown。JSON字段必须包含：reportTitle, consumerPainPoints, productAdvantages, productDisadvantages, operationSuggestions, copywritingSuggestions, serviceSuggestions, fullReport。目标类型：{targetType}，目标ID：{targetId}，输出语言：{language}。评论分析结果：{analysisResult}',
'["targetType","targetId","language","analysisResult"]', 1, 1, '商品/商家运营报告默认模板', now(), now()),
('默认营销文案模板', 'content', 'zh-CN',
'你是电商运营文案专家。请生成可直接给商家使用的营销文案。文案类型：{contentType}。风格：{styleType}。语言：{language}。目标类型：{targetType}，目标ID：{targetId}。补充要求：{extraRequirement}。输出正文即可，不要解释生成过程。',
'["contentType","styleType","language","targetType","targetId","extraRequirement"]', 1, 1, 'AI 文案默认模板', now(), now()),
('默认差评回复模板', 'negative_reply', 'zh-CN',
'你是电商客服主管。请为差评生成一段商家回复模板。回复要真诚、承担责任、给出解决路径，避免争辩和过度承诺。每条回复都必须针对这条评论单独生成，不要复用通用模板；需要自然提到客户反馈中的具体问题，不能编造评论中没有的信息。语气：{toneType}。语言：{language}。评论ID：{commentId}。平台评论ID：{reviewId}。商品ID：{productId}。评分：{reviewScore}。问题类型：{problemType}。评论标题：{commentTitle}。客户评论：{commentContent}。如果评论原文缺失，只能基于评分和问题类型表达歉意并引导客服核实。只输出回复内容。',
'["toneType","language","commentId","reviewId","productId","reviewScore","problemType","commentTitle","commentContent"]', 1, 1, '差评回复默认模板', now(), now()),
('默认评论翻译模板', 'translation', 'zh-CN',
'You are a precise ecommerce review translator. Translate the customer review into the target language. Preserve product facts, sentiment, complaint details, numbers, and named entities. Do not add explanations or invented details. Return only JSON with fields: translatedContent, sourceLanguage. target language: {targetLanguage}. commentId: {commentId}. reviewId: {reviewId}. productId: {productId}. reviewScore: {reviewScore}. title: {commentTitle}. review: {commentContent}.',
'["targetLanguage","commentId","reviewId","productId","reviewScore","commentTitle","commentContent"]', 1, 1, '评论翻译默认模板', now(), now()),
('默认商品对比模板', 'product_compare', 'zh-CN',
'你是中小电商商家的竞品评论分析顾问。请基于两个商品的评论分析结果生成对比报告。必须只输出JSON，不要输出Markdown。JSON字段必须包含：compareSummary, advantageAnalysis, riskAnalysis, operationSuggestions。所有字段值必须是字符串，不能返回嵌套对象或数组。左侧商品ID：{leftProductId}。右侧商品ID：{rightProductId}。输出语言：{language}。左侧商品分析结果：{leftAnalysis}。右侧商品分析结果：{rightAnalysis}',
'["leftProductId","rightProductId","language","leftAnalysis","rightAnalysis"]', 1, 1, '商品对比默认模板', now(), now()),
('默认评论 Shadow 分析模板', 'comment_analysis_shadow', 'zh-CN',
'你是电商评论质检分析助手。仅输出一个 JSON 对象，不要 Markdown、代码块或解释。字段必须为 sentiment、sentimentConfidence、primaryProblem、problems。sentiment 只能是 positive、neutral 或 negative。problems 最多 5 项，每项必须包含 type、confidence、evidence；evidence 必须是评论原文中的连续片段。primaryProblem 必须为 null 或 problems 中某项的 type。评分：{reviewScore}。评论原文：{reviewText}',
'["reviewScore","reviewText"]', 1, 1, '评论 Shadow 分析默认模板', now(), now()),
('Default Comment Shadow Analysis Template', 'comment_analysis_shadow', 'en-US',
'You are an ecommerce review quality analyst. Return exactly one JSON object with no Markdown, code fences, or explanation. Required fields: sentiment, sentimentConfidence, primaryProblem, problems. sentiment must be positive, neutral, or negative. problems may contain at most five items, each with type, confidence, and evidence. Every evidence value must be a contiguous excerpt from the review. primaryProblem must be null or match a problem type. Score: {reviewScore}. Review: {reviewText}',
'["reviewScore","reviewText"]', 1, 1, 'Default Shadow review analysis template', now(), now()),
('Modelo Padrao de Analise Shadow de Comentarios', 'comment_analysis_shadow', 'pt-BR',
'Voce e um analista de qualidade de comentarios de ecommerce. Retorne exatamente um objeto JSON, sem Markdown, bloco de codigo ou explicacao. Campos obrigatorios: sentiment, sentimentConfidence, primaryProblem, problems. sentiment deve ser positive, neutral ou negative. problems pode conter no maximo cinco itens, cada um com type, confidence e evidence. Cada evidence deve ser um trecho continuo da avaliacao. primaryProblem deve ser null ou corresponder ao type de um problema. Nota: {reviewScore}. Avaliacao: {reviewText}',
'["reviewScore","reviewText"]', 1, 1, 'Modelo padrao para analise Shadow de comentarios', now(), now())
on duplicate key update
template_content = values(template_content),
variable_schema = values(variable_schema),
default_flag = values(default_flag),
enabled = values(enabled),
remark = values(remark),
update_time = now();
