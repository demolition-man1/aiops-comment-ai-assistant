package com.aiops.service.impl;

import com.aiops.dto.CustomTagDTO;
import com.aiops.dto.CustomTagQueryDTO;
import com.aiops.entity.BizCustomTag;
import com.aiops.mapper.BizCustomTagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagLibraryServiceImplTest {

    @Mock
    private BizCustomTagMapper customTagMapper;

    private TagLibraryServiceImpl tagLibraryService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BizCustomTag.class);
        tagLibraryService = new TagLibraryServiceImpl(customTagMapper);
    }

    @Test
    void createTagTrimsInputAndEnablesTagByDefault() {
        CustomTagDTO dto = new CustomTagDTO();
        dto.setTagName(" 包装破损 ");
        dto.setTagGroup(" 物流体验 ");
        dto.setColor("#f97316");

        tagLibraryService.createTag(dto);

        ArgumentCaptor<BizCustomTag> tagCaptor = ArgumentCaptor.forClass(BizCustomTag.class);
        verify(customTagMapper).insert(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getTagName()).isEqualTo("包装破损");
        assertThat(tagCaptor.getValue().getTagGroup()).isEqualTo("物流体验");
        assertThat(tagCaptor.getValue().getEnabled()).isEqualTo(1);
    }

    @Test
    void pageTagsSearchesKeywordAndGroup() {
        when(customTagMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>());
        CustomTagQueryDTO queryDTO = new CustomTagQueryDTO();
        queryDTO.setKeyword("包装");
        queryDTO.setTagGroup("物流体验");
        queryDTO.setEnabled(1);

        tagLibraryService.pageTags(queryDTO);

        ArgumentCaptor<LambdaQueryWrapper<BizCustomTag>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(customTagMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("tagName");
        assertThat(sqlSegment).contains("tagGroup");
        assertThat(sqlSegment).contains("enabled");
    }

    @Test
    void updateStatusOnlyChangesEnabledAndUpdateTime() {
        BizCustomTag existing = new BizCustomTag();
        existing.setId(8L);
        existing.setTagName("包装破损");
        existing.setEnabled(1);
        when(customTagMapper.selectById(8L)).thenReturn(existing);

        tagLibraryService.updateStatus(8L, 0);

        ArgumentCaptor<BizCustomTag> tagCaptor = ArgumentCaptor.forClass(BizCustomTag.class);
        verify(customTagMapper).updateById(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getId()).isEqualTo(8L);
        assertThat(tagCaptor.getValue().getEnabled()).isZero();
        assertThat(tagCaptor.getValue().getUpdateTime()).isNotNull();
    }
}
