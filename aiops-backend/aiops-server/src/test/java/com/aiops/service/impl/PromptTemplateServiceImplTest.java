package com.aiops.service.impl;

import com.aiops.dto.PromptTemplateDTO;
import com.aiops.entity.SysPromptTemplate;
import com.aiops.mapper.SysPromptTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceImplTest {

    @Mock
    private SysPromptTemplateMapper promptTemplateMapper;

    private PromptTemplateServiceImpl promptTemplateService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), SysPromptTemplate.class);
        promptTemplateService = new PromptTemplateServiceImpl(promptTemplateMapper);
    }

    @Test
    void createDefaultTemplateClearsOtherDefaultsForSameBusinessAndLanguage() {
        PromptTemplateDTO dto = new PromptTemplateDTO();
        dto.setTemplateName(" English Reply ");
        dto.setBusinessType(" negative_reply ");
        dto.setLanguage(" en-US ");
        dto.setTemplateContent("Reply to {commentContent}");
        dto.setDefaultFlag(1);

        SysPromptTemplate oldDefault = new SysPromptTemplate();
        oldDefault.setId(7L);
        oldDefault.setBusinessType("negative_reply");
        oldDefault.setLanguage("en-US");
        oldDefault.setDefaultFlag(1);
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(oldDefault));

        promptTemplateService.createTemplate(dto);

        ArgumentCaptor<SysPromptTemplate> insertCaptor = ArgumentCaptor.forClass(SysPromptTemplate.class);
        verify(promptTemplateMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getTemplateName()).isEqualTo("English Reply");
        assertThat(insertCaptor.getValue().getBusinessType()).isEqualTo("negative_reply");
        assertThat(insertCaptor.getValue().getLanguage()).isEqualTo("en-US");
        assertThat(insertCaptor.getValue().getDefaultFlag()).isEqualTo(1);

        ArgumentCaptor<SysPromptTemplate> updateCaptor = ArgumentCaptor.forClass(SysPromptTemplate.class);
        verify(promptTemplateMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(7L);
        assertThat(updateCaptor.getValue().getDefaultFlag()).isZero();
    }

    @Test
    void findDefaultTemplateFallsBackToChineseTemplate() {
        SysPromptTemplate template = new SysPromptTemplate();
        template.setId(9L);
        template.setBusinessType("report");
        template.setLanguage("zh-CN");
        template.setEnabled(1);
        template.setDefaultFlag(1);
        when(promptTemplateMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null)
                .thenReturn(template);

        Optional<SysPromptTemplate> result = promptTemplateService.findDefaultTemplate("report", "en-US");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(9L);
    }
}
