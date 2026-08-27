package com.aiops.service;

import com.aiops.dto.PromptTemplateDTO;
import com.aiops.dto.PromptTemplateQueryDTO;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.result.PageResult;
import com.aiops.vo.PromptTemplateVO;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateService {
    PageResult<PromptTemplateVO> pageTemplates(PromptTemplateQueryDTO queryDTO);

    List<PromptTemplateVO> activeTemplates(String businessType, String language);

    PromptTemplateVO createTemplate(PromptTemplateDTO templateDTO);

    PromptTemplateVO updateTemplate(Long templateId, PromptTemplateDTO templateDTO);

    PromptTemplateVO updateStatus(Long templateId, Integer enabled);

    PromptTemplateVO setDefault(Long templateId);

    Optional<SysPromptTemplate> findDefaultTemplate(String businessType, String language);
}
