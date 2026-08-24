package com.aiops.service;

import com.aiops.dto.CrawlerImportDTO;
import com.aiops.dto.CsvImportDTO;
import com.aiops.vo.TaskVO;

public interface DataImportService {
    TaskVO importCsv(CsvImportDTO csvImportDTO);

    TaskVO importByCrawler(CrawlerImportDTO crawlerImportDTO);

    TaskVO getImportTask(Long taskId);

    TaskVO getImportTask(Long taskId, String importType);
}
