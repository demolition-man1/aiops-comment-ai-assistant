package com.aiops.service;

import com.aiops.dto.AnalysisTaskCreateDTO;
import com.aiops.dto.ProductCompareDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.AnalysisResultVO;
import com.aiops.vo.ProductCompareReportVO;
import com.aiops.vo.TaskVO;

public interface AnalysisService {
    TaskVO createAnalysisTask(AnalysisTaskCreateDTO createDTO);

    TaskVO getTask(Long taskId);

    AnalysisResultVO getProductAnalysis(String productId);

    AnalysisResultVO getSellerAnalysis(String sellerId);

    ProductCompareReportVO compareProducts(ProductCompareDTO compareDTO);

    PageResult<ProductCompareReportVO> pageProductCompareReports(String leftProductId, String rightProductId,
                                                                 Integer pageNum, Integer pageSize);

    ProductCompareReportVO getProductCompareReport(Long reportId);
}
