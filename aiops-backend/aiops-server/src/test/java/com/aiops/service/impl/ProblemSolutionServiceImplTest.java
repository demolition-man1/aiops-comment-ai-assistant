package com.aiops.service.impl;

import com.aiops.dto.ProblemSolutionDTO;
import com.aiops.dto.ProblemSolutionQueryDTO;
import com.aiops.entity.BizProblemSolution;
import com.aiops.mapper.BizProblemSolutionMapper;
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
class ProblemSolutionServiceImplTest {

    @Mock
    private BizProblemSolutionMapper problemSolutionMapper;

    private ProblemSolutionServiceImpl problemSolutionService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BizProblemSolution.class);
        problemSolutionService = new ProblemSolutionServiceImpl(problemSolutionMapper);
    }

    @Test
    void createSolutionTrimsCoreFieldsAndEnablesByDefault() {
        ProblemSolutionDTO dto = new ProblemSolutionDTO();
        dto.setProblemType(" logistics ");
        dto.setCategoryNameEn(" bed_bath_table ");
        dto.setSolutionTitle(" Improve packaging ");
        dto.setSolutionContent("Use thicker boxes and add padding.");
        dto.setKeywords("package,box");

        problemSolutionService.createSolution(dto);

        ArgumentCaptor<BizProblemSolution> solutionCaptor = ArgumentCaptor.forClass(BizProblemSolution.class);
        verify(problemSolutionMapper).insert(solutionCaptor.capture());
        assertThat(solutionCaptor.getValue().getProblemType()).isEqualTo("logistics");
        assertThat(solutionCaptor.getValue().getCategoryNameEn()).isEqualTo("bed_bath_table");
        assertThat(solutionCaptor.getValue().getSolutionTitle()).isEqualTo("Improve packaging");
        assertThat(solutionCaptor.getValue().getEnabled()).isEqualTo(1);
    }

    @Test
    void pageSolutionsSearchesProblemCategoryAndKeyword() {
        when(problemSolutionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(new Page<>());
        ProblemSolutionQueryDTO queryDTO = new ProblemSolutionQueryDTO();
        queryDTO.setProblemType("logistics");
        queryDTO.setCategoryNameEn("bed_bath_table");
        queryDTO.setKeyword("packaging");
        queryDTO.setEnabled(1);

        problemSolutionService.pageSolutions(queryDTO);

        ArgumentCaptor<LambdaQueryWrapper<BizProblemSolution>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemSolutionMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("problemType");
        assertThat(sqlSegment).contains("categoryNameEn");
        assertThat(sqlSegment).contains("solutionTitle");
        assertThat(sqlSegment).contains("keywords");
    }

    @Test
    void recommendSolutionsUsesEnabledProblemCategoryAndKeywordSignals() {
        when(problemSolutionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());

        problemSolutionService.recommendSolutions("logistics", "bed_bath_table", "package damaged");

        ArgumentCaptor<LambdaQueryWrapper<BizProblemSolution>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(problemSolutionMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("enabled");
        assertThat(sqlSegment).contains("problemType");
        assertThat(sqlSegment).contains("categoryNameEn");
        assertThat(sqlSegment).contains("keywords");
    }
}
