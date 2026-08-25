package com.aiops.service;

import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.dto.CsvImportPreflightDTO;
import com.aiops.vo.CsvImportPreflightVO;
import com.aiops.vo.TaskVO;

public interface DataImportService {
    CsvImportPreflightVO preflightCsv(CsvImportPreflightDTO preflightDTO);

    TaskVO importCsv(CsvImportDTO csvImportDTO);

    TaskVO importSample();

    TaskVO importByCrawler(CrawlerImportDTO crawlerImportDTO);

    TaskVO getImportTask(Long taskId);

    TaskVO getImportTask(Long taskId, String importType);
}
