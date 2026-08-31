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
import com.aiops.service.ReportArchiveService;
import com.aiops.vo.ReportArchiveVO;
import com.aiops.vo.ReportEvidenceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportArchiveServiceImpl implements ReportArchiveService {

    private static final String STATUS_ARCHIVED = "archived";
    private static final String STATUS_RESTORED = "restored";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_ARCHIVED, STATUS_RESTORED);

    private final BizReportArchiveMapper reportArchiveMapper;
    private final BizOperationReportMapper operationReportMapper;
    private final BizOperationReportEvidenceMapper operationReportEvidenceMapper;

    @Override
    public PageResult<ReportArchiveVO> pageArchives(ReportArchiveQueryDTO queryDTO) {
        ReportArchiveQueryDTO query = queryDTO == null ? new ReportArchiveQueryDTO() : queryDTO;
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());
        String targetType = blankToNull(query.getTargetType());
        String targetId = blankToNull(query.getTargetId());
        String keyword = blankToNull(query.getKeyword());
        String archiveStatus = normalizeOptionalStatus(query.getArchiveStatus());
        LocalDateTime startTime = parseDateTime(query.getStartTime(), false);
        LocalDateTime endTime = parseDateTime(query.getEndTime(), true);
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(400, "归档开始时间不能晚于结束时间");
        }

        LambdaQueryWrapper<BizReportArchive> wrapper = new LambdaQueryWrapper<BizReportArchive>()
                .eq(targetType != null, BizReportArchive::getTargetType, targetType)
                .eq(targetId != null, BizReportArchive::getTargetId, targetId)
                .eq(archiveStatus != null, BizReportArchive::getArchiveStatus, archiveStatus)
                .and(keyword != null, condition -> condition
                        .like(BizReportArchive::getReportTitle, keyword)
                        .or()
                        .like(BizReportArchive::getTargetId, keyword))
                .ge(startTime != null, BizReportArchive::getArchiveTime, startTime)
                .le(endTime != null, BizReportArchive::getArchiveTime, endTime)
                .orderByDesc(BizReportArchive::getArchiveTime)
                .orderByDesc(BizReportArchive::getId);
        Page<BizReportArchive> page = reportArchiveMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ReportArchiveVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional
    public ReportArchiveVO archiveReport(Long reportId, ReportArchiveCreateDTO createDTO) {
        if (reportId == null) {
            throw new BusinessException(400, "报告 ID 不能为空");
        }
        BizReportArchive existing = findBySourceReportId(reportId);
        if (existing != null) {
            if (STATUS_RESTORED.equals(existing.getArchiveStatus())) {
                LocalDateTime now = LocalDateTime.now();
                existing.setArchiveStatus(STATUS_ARCHIVED);
                existing.setArchivedBy(BaseContext.getCurrentId());
                existing.setArchiveTime(now);
                existing.setUpdateTime(now);
                String remark = blankToNull(createDTO == null ? null : createDTO.getRemark());
                if (remark != null) {
                    existing.setArchiveRemark(remark);
                }
                reportArchiveMapper.updateById(existing);
            }
            return toVO(existing);
        }

        BizOperationReport report = operationReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "AI 运营报告不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        BizReportArchive archive = copyFromReport(report);
        archive.setArchiveStatus(STATUS_ARCHIVED);
        archive.setArchiveRemark(blankToNull(createDTO == null ? null : createDTO.getRemark()));
        archive.setArchivedBy(BaseContext.getCurrentId());
        archive.setArchiveTime(now);
        archive.setCreateTime(now);
        archive.setUpdateTime(now);
        try {
            reportArchiveMapper.insert(archive);
        } catch (DuplicateKeyException exception) {
            BizReportArchive concurrentArchive = findBySourceReportId(reportId);
            if (concurrentArchive == null) {
                throw exception;
            }
            return toVO(concurrentArchive);
        }
        return toVO(archive);
    }

    @Override
    public ReportArchiveVO getArchive(Long archiveId) {
        return toVO(requireArchive(archiveId));
    }

    @Override
    public ReportArchiveVO updateStatus(Long archiveId, ReportArchiveStatusDTO statusDTO) {
        BizReportArchive archive = requireArchive(archiveId);
        String archiveStatus = normalizeRequiredStatus(statusDTO == null ? null : statusDTO.getArchiveStatus());
        LocalDateTime now = LocalDateTime.now();
        archive.setArchiveStatus(archiveStatus);
        archive.setUpdateTime(now);
        if (STATUS_ARCHIVED.equals(archiveStatus)) {
            archive.setArchivedBy(BaseContext.getCurrentId());
            archive.setArchiveTime(now);
        }
        reportArchiveMapper.updateById(archive);
        return toVO(archive);
    }

    private BizReportArchive findBySourceReportId(Long reportId) {
        return reportArchiveMapper.selectOne(new LambdaQueryWrapper<BizReportArchive>()
                .eq(BizReportArchive::getSourceReportId, reportId)
                .last("limit 1"));
    }

    private BizReportArchive requireArchive(Long archiveId) {
        if (archiveId == null) {
            throw new BusinessException(400, "归档 ID 不能为空");
        }
        BizReportArchive archive = reportArchiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(404, "报告归档不存在");
        }
        return archive;
    }

    private BizReportArchive copyFromReport(BizOperationReport report) {
        BizReportArchive archive = new BizReportArchive();
        archive.setSourceReportId(report.getId());
        archive.setTaskId(report.getTaskId());
        archive.setTargetType(report.getTargetType());
        archive.setTargetId(report.getTargetId());
        archive.setReportTitle(report.getReportTitle());
        archive.setConsumerPainPoints(report.getConsumerPainPoints());
        archive.setProductAdvantages(report.getProductAdvantages());
        archive.setProductDisadvantages(report.getProductDisadvantages());
        archive.setOperationSuggestions(report.getOperationSuggestions());
        archive.setCopywritingSuggestions(report.getCopywritingSuggestions());
        archive.setServiceSuggestions(report.getServiceSuggestions());
        archive.setRiskTips(report.getRiskTips());
        archive.setFullReport(report.getFullReport());
        archive.setModelName(report.getModelName());
        archive.setReportCreateTime(report.getCreateTime());
        return archive;
    }

    private ReportArchiveVO toVO(BizReportArchive archive) {
        return new ReportArchiveVO(archive.getId(), archive.getSourceReportId(), archive.getTaskId(),
                archive.getTargetType(), archive.getTargetId(), archive.getReportTitle(),
                archive.getConsumerPainPoints(), archive.getProductAdvantages(), archive.getProductDisadvantages(),
                archive.getOperationSuggestions(), archive.getCopywritingSuggestions(), archive.getServiceSuggestions(),
                archive.getRiskTips(), archive.getFullReport(), archive.getModelName(), archive.getReportCreateTime(),
                archive.getArchiveStatus(), archive.getArchiveRemark(), archive.getArchivedBy(), archive.getArchiveTime(),
                archive.getCreateTime(), archive.getUpdateTime(), reportEvidence(archive.getSourceReportId()));
    }

    private List<ReportEvidenceVO> reportEvidence(Long reportId) {
        if (reportId == null) {
            return List.of();
        }
        List<BizOperationReportEvidence> records = operationReportEvidenceMapper.selectByReportId(reportId);
        if (records == null) {
            return List.of();
        }
        return records.stream()
                .map(record -> new ReportEvidenceVO(record.getSourceType(), record.getSourceId(),
                        record.getSourceTitle(), record.getRelevanceScore(), record.getRetrievalVersion()))
                .toList();
    }

    private String normalizeOptionalStatus(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalizeRequiredStatus(normalized);
    }

    private String normalizeRequiredStatus(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new BusinessException(400, "归档状态不能为空");
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException(400, "归档状态仅支持 archived 或 restored");
        }
        return normalized;
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            if (normalized.length() == 10) {
                LocalDate date = LocalDate.parse(normalized);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            return LocalDateTime.parse(normalized.replace(' ', 'T'));
        } catch (DateTimeParseException exception) {
            throw new BusinessException(400, "归档时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
