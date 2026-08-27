package com.aiops.service.impl;

import com.aiops.dto.PromptTemplateDTO;
import com.aiops.dto.PromptTemplateQueryDTO;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.SysPromptTemplateMapper;
import com.aiops.result.PageResult;
import com.aiops.service.PromptTemplateService;
import com.aiops.vo.PromptTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final SysPromptTemplateMapper promptTemplateMapper;

    @Override
    public PageResult<PromptTemplateVO> pageTemplates(PromptTemplateQueryDTO queryDTO) {
        PromptTemplateQueryDTO query = queryDTO == null ? new PromptTemplateQueryDTO() : queryDTO;
        String keyword = blankToNull(query.getKeyword());
        String businessType = blankToNull(query.getBusinessType());
        String language = blankToNull(query.getLanguage());
        LambdaQueryWrapper<SysPromptTemplate> wrapper = new LambdaQueryWrapper<SysPromptTemplate>()
                .eq(businessType != null, SysPromptTemplate::getBusinessType, businessType)
                .eq(language != null, SysPromptTemplate::getLanguage, language)
                .eq(query.getEnabled() != null, SysPromptTemplate::getEnabled, query.getEnabled())
                .and(keyword != null, condition -> condition
                        .like(SysPromptTemplate::getTemplateName, keyword)
                        .or()
                        .like(SysPromptTemplate::getRemark, keyword))
                .orderByDesc(SysPromptTemplate::getDefaultFlag)
                .orderByDesc(SysPromptTemplate::getUpdateTime);
        Page<SysPromptTemplate> page = promptTemplateMapper.selectPage(new Page<>(
                normalizePageNum(query.getPageNum()), normalizePageSize(query.getPageSize())), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public List<PromptTemplateVO> activeTemplates(String businessType, String language) {
        String normalizedBusinessType = blankToNull(businessType);
        String normalizedLanguage = blankToNull(language);
        return promptTemplateMapper.selectList(new LambdaQueryWrapper<SysPromptTemplate>()
                        .eq(SysPromptTemplate::getEnabled, 1)
                        .eq(normalizedBusinessType != null, SysPromptTemplate::getBusinessType, normalizedBusinessType)
                        .eq(normalizedLanguage != null, SysPromptTemplate::getLanguage, normalizedLanguage)
                        .orderByDesc(SysPromptTemplate::getDefaultFlag)
                        .orderByDesc(SysPromptTemplate::getUpdateTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public PromptTemplateVO createTemplate(PromptTemplateDTO templateDTO) {
        validate(templateDTO);
        SysPromptTemplate template = new SysPromptTemplate();
        copyToEntity(templateDTO, template);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        promptTemplateMapper.insert(template);
        if (Integer.valueOf(1).equals(template.getDefaultFlag())) {
            clearOtherDefaults(template);
        }
        return toVO(template);
    }

    @Override
    @Transactional
    public PromptTemplateVO updateTemplate(Long templateId, PromptTemplateDTO templateDTO) {
        validate(templateDTO);
        SysPromptTemplate template = requireTemplate(templateId);
        copyToEntity(templateDTO, template);
        template.setUpdateTime(LocalDateTime.now());
        promptTemplateMapper.updateById(template);
        if (Integer.valueOf(1).equals(template.getDefaultFlag())) {
            clearOtherDefaults(template);
        }
        return toVO(template);
    }

    @Override
    public PromptTemplateVO updateStatus(Long templateId, Integer enabled) {
        SysPromptTemplate template = requireTemplate(templateId);
        template.setEnabled(normalizeEnabled(enabled));
        template.setUpdateTime(LocalDateTime.now());
        promptTemplateMapper.updateById(template);
        return toVO(template);
    }

    @Override
    @Transactional
    public PromptTemplateVO setDefault(Long templateId) {
        SysPromptTemplate template = requireTemplate(templateId);
        template.setEnabled(1);
        template.setDefaultFlag(1);
        template.setUpdateTime(LocalDateTime.now());
        promptTemplateMapper.updateById(template);
        clearOtherDefaults(template);
        return toVO(template);
    }

    @Override
    public Optional<SysPromptTemplate> findDefaultTemplate(String businessType, String language) {
        String normalizedBusinessType = blankToNull(businessType);
        if (normalizedBusinessType == null) {
            return Optional.empty();
        }
        String normalizedLanguage = blankToDefault(language, "zh-CN");
        Optional<SysPromptTemplate> exact = findDefault(normalizedBusinessType, normalizedLanguage);
        if (exact.isPresent()) {
            return exact;
        }
        if (!"zh-CN".equals(normalizedLanguage)) {
            Optional<SysPromptTemplate> fallback = findDefault(normalizedBusinessType, "zh-CN");
            if (fallback.isPresent()) {
                return fallback;
            }
        }
        return promptTemplateMapper.selectList(new LambdaQueryWrapper<SysPromptTemplate>()
                        .eq(SysPromptTemplate::getBusinessType, normalizedBusinessType)
                        .eq(SysPromptTemplate::getEnabled, 1)
                        .orderByDesc(SysPromptTemplate::getDefaultFlag)
                        .orderByDesc(SysPromptTemplate::getUpdateTime)
                        .last("limit 1"))
                .stream()
                .findFirst();
    }

    private Optional<SysPromptTemplate> findDefault(String businessType, String language) {
        return Optional.ofNullable(promptTemplateMapper.selectOne(new LambdaQueryWrapper<SysPromptTemplate>()
                .eq(SysPromptTemplate::getBusinessType, businessType)
                .eq(SysPromptTemplate::getLanguage, language)
                .eq(SysPromptTemplate::getDefaultFlag, 1)
                .eq(SysPromptTemplate::getEnabled, 1)
                .orderByDesc(SysPromptTemplate::getUpdateTime)
                .last("limit 1")));
    }

    private void validate(PromptTemplateDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "Prompt 模板参数不能为空");
        }
        if (blankToNull(dto.getTemplateName()) == null) {
            throw new BusinessException(400, "模板名称不能为空");
        }
        if (blankToNull(dto.getBusinessType()) == null) {
            throw new BusinessException(400, "业务类型不能为空");
        }
        if (blankToNull(dto.getLanguage()) == null) {
            throw new BusinessException(400, "语言不能为空");
        }
        if (blankToNull(dto.getTemplateContent()) == null) {
            throw new BusinessException(400, "模板内容不能为空");
        }
    }

    private void copyToEntity(PromptTemplateDTO dto, SysPromptTemplate template) {
        template.setTemplateName(blankToNull(dto.getTemplateName()));
        template.setBusinessType(blankToNull(dto.getBusinessType()));
        template.setLanguage(blankToDefault(dto.getLanguage(), "zh-CN"));
        template.setTemplateContent(blankToNull(dto.getTemplateContent()));
        template.setVariableSchema(blankToNull(dto.getVariableSchema()));
        template.setDefaultFlag(Integer.valueOf(1).equals(dto.getDefaultFlag()) ? 1 : 0);
        template.setEnabled(normalizeEnabled(dto.getEnabled()));
        template.setRemark(blankToNull(dto.getRemark()));
    }

    private void clearOtherDefaults(SysPromptTemplate template) {
        List<SysPromptTemplate> defaults = promptTemplateMapper.selectList(new LambdaQueryWrapper<SysPromptTemplate>()
                .eq(SysPromptTemplate::getBusinessType, template.getBusinessType())
                .eq(SysPromptTemplate::getLanguage, template.getLanguage())
                .eq(SysPromptTemplate::getDefaultFlag, 1)
                .ne(template.getId() != null, SysPromptTemplate::getId, template.getId()));
        for (SysPromptTemplate other : defaults) {
            other.setDefaultFlag(0);
            other.setUpdateTime(LocalDateTime.now());
            promptTemplateMapper.updateById(other);
        }
    }

    private SysPromptTemplate requireTemplate(Long templateId) {
        SysPromptTemplate template = promptTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(404, "Prompt 模板不存在");
        }
        return template;
    }

    private PromptTemplateVO toVO(SysPromptTemplate template) {
        return new PromptTemplateVO(template.getId(), template.getTemplateName(), template.getBusinessType(),
                template.getLanguage(), template.getTemplateContent(), template.getVariableSchema(),
                template.getDefaultFlag(), template.getEnabled(), template.getRemark(),
                template.getCreateTime(), template.getUpdateTime());
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private int normalizeEnabled(Integer enabled) {
        return Integer.valueOf(0).equals(enabled) ? 0 : 1;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
