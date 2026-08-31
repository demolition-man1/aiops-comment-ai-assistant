package com.aiops.service.impl;

import com.aiops.context.BaseContext;
import com.aiops.dto.ReportArchiveCreateDTO;
import com.aiops.dto.ReportArchiveQueryDTO;
import com.aiops.dto.ReportArchiveStatusDTO;
import com.aiops.entity.BizOperationReport;
import com.aiops.entity.BizOperationReportEvidence;
import com.aiops.entity.BizReportArchive;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizOperationReportMapper;
import com.aiops.mapper.BizOperationReportEvidenceMapper;
import com.aiops.mapper.BizReportArchiveMapper;
import com.aiops.result.PageResult;
import com.aiops.vo.ReportArchiveVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportArchiveServiceImplTest {

    @Mock
    private BizReportArchiveMapper reportArchiveMapper;

    @Mock
    private BizOperationReportMapper operationReportMapper;

    @Mock
    private BizOperationReportEvidenceMapper operationReportEvidenceMapper;

    private ReportArchiveServiceImpl reportArchiveService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), ""), BizReportArchive.class);
        reportArchiveService = new ReportArchiveServiceImpl(reportArchiveMapper, operationReportMapper,
                operationReportEvidenceMapper);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void archiveReportCopiesSourceIntoStableSnapshot() {
        LocalDateTime reportCreatedAt = LocalDateTime.of(2026, 8, 20, 10, 30);
        BizOperationReport source = sourceReport(reportCreatedAt);
        when(reportArchiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(operationReportMapper.selectById(41L)).thenReturn(source);
        when(reportArchiveMapper.insert(any(BizReportArchive.class))).thenAnswer(invocation -> {
            BizReportArchive archive = invocation.getArgument(0);
            archive.setId(101L);
            return 1;
        });
        BaseContext.setCurrentId(9L);

        ReportArchiveCreateDTO createDTO = new ReportArchiveCreateDTO();
        createDTO.setRemark("Monthly review");
        ReportArchiveVO result = reportArchiveService.archiveReport(41L, createDTO);

        assertThat(result.getArchiveId()).isEqualTo(101L);
        assertThat(result.getSourceReportId()).isEqualTo(41L);
        assertThat(result.getReportTitle()).isEqualTo("August operations report");
        assertThat(result.getFullReport()).isEqualTo("Full report snapshot");
        assertThat(result.getArchiveStatus()).isEqualTo("archived");
        assertThat(result.getArchiveRemark()).isEqualTo("Monthly review");
        assertThat(result.getArchivedBy()).isEqualTo(9L);
        assertThat(result.getReportCreateTime()).isEqualTo(reportCreatedAt);

        ArgumentCaptor<BizReportArchive> captor = ArgumentCaptor.forClass(BizReportArchive.class);
        verify(reportArchiveMapper).insert(captor.capture());
        assertThat(captor.getValue().getConsumerPainPoints()).isEqualTo("Slow delivery");
        assertThat(captor.getValue().getProductAdvantages()).isEqualTo("Reliable quality");
        assertThat(captor.getValue().getArchiveTime()).isNotNull();
        assertThat(captor.getValue().getCreateTime()).isNotNull();
        assertThat(captor.getValue().getUpdateTime()).isNotNull();
    }

    @Test
    void archiveReportIsIdempotentWhenSourceIsAlreadyArchived() {
        BizReportArchive existing = archivedReport();
        when(reportArchiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        ReportArchiveVO result = reportArchiveService.archiveReport(41L, new ReportArchiveCreateDTO());

        assertThat(result.getArchiveId()).isEqualTo(101L);
        verify(operationReportMapper, never()).selectById(any());
        verify(reportArchiveMapper, never()).insert(any(BizReportArchive.class));
        verify(reportArchiveMapper, never()).updateById(any(BizReportArchive.class));
    }

    @Test
    void archiveDetailReadsOptionalEvidenceFromItsSourceReport() {
        BizOperationReportEvidence evidence = new BizOperationReportEvidence();
        evidence.setSourceType("review_evidence");
        evidence.setSourceId(31L);
        evidence.setSourceTitle("Review #31");
        evidence.setRelevanceScore(0.91);
        evidence.setRetrievalVersion("review-evidence-v1");
        when(reportArchiveMapper.selectById(101L)).thenReturn(archivedReport());
        when(operationReportEvidenceMapper.selectByReportId(41L)).thenReturn(List.of(evidence));

        ReportArchiveVO result = reportArchiveService.getArchive(101L);

        assertThat(result.getEvidence()).singleElement().satisfies(item -> {
            assertThat(item.getSourceType()).isEqualTo("review_evidence");
            assertThat(item.getSourceId()).isEqualTo(31L);
        });
    }

    @Test
    void pageArchivesAppliesHistoricalReportFiltersAndClampsPageSize() {
        ReportArchiveQueryDTO query = new ReportArchiveQueryDTO();
        query.setPageNum(2);
        query.setPageSize(200);
        query.setTargetType(" product ");
        query.setTargetId(" product-a ");
        query.setKeyword(" August ");
        query.setArchiveStatus(" archived ");
        query.setStartTime("2026-08-01 00:00:00");
        query.setEndTime("2026-08-31 23:59:59");

        when(reportArchiveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<BizReportArchive> page = invocation.getArgument(0);
                    page.setRecords(List.of(archivedReport()));
                    page.setTotal(1);
                    return page;
                });

        PageResult<ReportArchiveVO> result = reportArchiveService.pageArchives(query);

        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).extracting(ReportArchiveVO::getArchiveId).containsExactly(101L);

        ArgumentCaptor<LambdaQueryWrapper<BizReportArchive>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(reportArchiveMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sql = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sql).contains("targetType", "targetId", "reportTitle", "archiveStatus", "archiveTime");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains("product", "product-a", "%August%", "archived",
                        LocalDateTime.of(2026, 8, 1, 0, 0),
                        LocalDateTime.of(2026, 8, 31, 23, 59, 59));
    }

    @Test
    void restoredArchiveCanBeArchivedAgain() {
        BizReportArchive existing = archivedReport();
        existing.setArchiveStatus("restored");
        when(reportArchiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        BaseContext.setCurrentId(12L);

        ReportArchiveVO result = reportArchiveService.archiveReport(41L, new ReportArchiveCreateDTO());

        assertThat(result.getArchiveStatus()).isEqualTo("archived");
        assertThat(result.getArchivedBy()).isEqualTo(12L);
        verify(reportArchiveMapper).updateById(existing);
    }

    @Test
    void concurrentDuplicateArchiveReturnsTheExistingSnapshot() {
        BizReportArchive existing = archivedReport();
        when(reportArchiveMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null)
                .thenReturn(existing);
        when(operationReportMapper.selectById(41L))
                .thenReturn(sourceReport(LocalDateTime.of(2026, 8, 20, 10, 30)));
        when(reportArchiveMapper.insert(any(BizReportArchive.class)))
                .thenThrow(new DuplicateKeyException("uk_report_archive_source"));

        ReportArchiveVO result = reportArchiveService.archiveReport(41L, new ReportArchiveCreateDTO());

        assertThat(result.getArchiveId()).isEqualTo(101L);
        verify(reportArchiveMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void updateStatusRejectsUnknownArchiveStatus() {
        when(reportArchiveMapper.selectById(101L)).thenReturn(archivedReport());
        ReportArchiveStatusDTO statusDTO = new ReportArchiveStatusDTO();
        statusDTO.setArchiveStatus("deleted");

        assertThatThrownBy(() -> reportArchiveService.updateStatus(101L, statusDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("归档状态");

        verify(reportArchiveMapper, never()).updateById(any(BizReportArchive.class));
    }

    private BizOperationReport sourceReport(LocalDateTime createTime) {
        BizOperationReport report = new BizOperationReport();
        report.setId(41L);
        report.setTaskId(7L);
        report.setTargetType("product");
        report.setTargetId("product-a");
        report.setReportTitle("August operations report");
        report.setConsumerPainPoints("Slow delivery");
        report.setProductAdvantages("Reliable quality");
        report.setProductDisadvantages("Limited packaging");
        report.setOperationSuggestions("Improve logistics");
        report.setCopywritingSuggestions("Highlight quality");
        report.setServiceSuggestions("Reply within 24 hours");
        report.setRiskTips("Monitor late deliveries");
        report.setFullReport("Full report snapshot");
        report.setModelName("deepseek-chat");
        report.setCreateTime(createTime);
        return report;
    }

    private BizReportArchive archivedReport() {
        BizReportArchive archive = new BizReportArchive();
        archive.setId(101L);
        archive.setSourceReportId(41L);
        archive.setTargetType("product");
        archive.setTargetId("product-a");
        archive.setReportTitle("August operations report");
        archive.setFullReport("Full report snapshot");
        archive.setModelName("deepseek-chat");
        archive.setArchiveStatus("archived");
        archive.setArchiveTime(LocalDateTime.of(2026, 8, 21, 9, 0));
        archive.setReportCreateTime(LocalDateTime.of(2026, 8, 20, 10, 30));
        return archive;
    }
}
