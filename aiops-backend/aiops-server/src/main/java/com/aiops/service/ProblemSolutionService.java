package com.aiops.service;

import com.aiops.dto.ProblemSolutionDTO;
import com.aiops.dto.ProblemSolutionQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.ProblemSolutionVO;

import java.util.List;

public interface ProblemSolutionService {
    PageResult<ProblemSolutionVO> pageSolutions(ProblemSolutionQueryDTO queryDTO);

    List<ProblemSolutionVO> recommendSolutions(String problemType, String categoryNameEn, String keyword);

    ProblemSolutionVO createSolution(ProblemSolutionDTO problemSolutionDTO);

    ProblemSolutionVO updateSolution(Long solutionId, ProblemSolutionDTO problemSolutionDTO);

    ProblemSolutionVO updateStatus(Long solutionId, Integer enabled);
}
