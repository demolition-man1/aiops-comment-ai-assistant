package com.aiops.service.impl;

import com.aiops.converter.AnalysisJsonConverter;
import com.aiops.dto.CommentQueryDTO;
import com.aiops.dto.CommentTagUpdateDTO;
import com.aiops.entity.BizComment;
import com.aiops.mapper.BizCommentMapper;
import com.aiops.service.CacheService;
import com.aiops.vo.CommentVO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private BizCommentMapper commentMapper;

    @Mock
    private CacheService cacheService;

    private CommentServiceImpl commentService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("com.aiops.mapper.BizCommentMapper");
        TableInfoHelper.initTableInfo(assistant, BizComment.class);
    }

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(
                commentMapper,
                new AnalysisJsonConverter(new ObjectMapper()),
                cacheService
        );
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void pageCommentsUsesTrimmedPrefixMatchForShortProductId() {
        CommentQueryDTO queryDTO = new CommentQueryDTO();
        queryDTO.setProductId(" 00066f42 ");
        when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<BizComment>(1, 10));

        commentService.pageComments(queryDTO);

        ArgumentCaptor<LambdaQueryWrapper<BizComment>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(commentMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<BizComment> wrapper = wrapperCaptor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("LIKE");
        assertThat(wrapper.getParamNameValuePairs()).containsValue("00066f42%");
    }

    @Test
    void updateTagsStoresManualTagsAndReturnsEffectiveProblemType() {
        BizComment comment = new BizComment();
        comment.setId(7L);
        comment.setProductId("product-1");
        comment.setSellerId("seller-1");
        comment.setProblemType("logistics");
        comment.setReviewContent("delivery was late");
        comment.setIsNegative(1);
        when(commentMapper.selectById(7L)).thenReturn(comment);
        when(commentMapper.updateById(any(BizComment.class))).thenReturn(1);

        CommentTagUpdateDTO updateDTO = new CommentTagUpdateDTO();
        updateDTO.setManualProblemType("quality");
        updateDTO.setCustomTags(List.of("fragile", "vip"));

        CommentVO result = commentService.updateTags(7L, updateDTO);

        assertThat(result.getSystemProblemType()).isEqualTo("logistics");
        assertThat(result.getManualProblemType()).isEqualTo("quality");
        assertThat(result.getEffectiveProblemType()).isEqualTo("quality");
        assertThat(result.getCustomTags()).containsExactly("fragile", "vip");

        ArgumentCaptor<BizComment> captor = ArgumentCaptor.forClass(BizComment.class);
        verify(commentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getCustomTags()).isEqualTo("[\"fragile\",\"vip\"]");
        assertThat(captor.getValue().getTagUpdateTime()).isNotNull();
        verify(cacheService).delete("analysis:product:product-1");
        verify(cacheService).delete("analysis:seller:seller-1");
    }
}
