package com.aiops.service;

import com.aiops.dto.ReportArchiveCreateDTO;
import com.aiops.dto.ReportArchiveQueryDTO;
import com.aiops.dto.ReportArchiveStatusDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.ReportArchiveVO;

public interface ReportArchiveService {
    PageResult<ReportArchiveVO> pageArchives(ReportArchiveQueryDTO queryDTO);

    ReportArchiveVO archiveReport(Long reportId, ReportArchiveCreateDTO createDTO);

    ReportArchiveVO getArchive(Long archiveId);

    ReportArchiveVO updateStatus(Long archiveId, ReportArchiveStatusDTO statusDTO);
}
