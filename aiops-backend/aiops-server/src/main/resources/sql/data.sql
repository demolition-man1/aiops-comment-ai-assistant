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
