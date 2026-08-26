package com.aiops.service.impl;

import com.aiops.dto.ProblemSolutionDTO;
import com.aiops.dto.ProblemSolutionQueryDTO;
import com.aiops.entity.BizProblemSolution;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizProblemSolutionMapper;
import com.aiops.result.PageResult;
import com.aiops.service.ProblemSolutionService;
import com.aiops.vo.ProblemSolutionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemSolutionServiceImpl implements ProblemSolutionService {

    private final BizProblemSolutionMapper problemSolutionMapper;

    @Override
    public PageResult<ProblemSolutionVO> pageSolutions(ProblemSolutionQueryDTO queryDTO) {
        ProblemSolutionQueryDTO query = queryDTO == null ? new ProblemSolutionQueryDTO() : queryDTO;
        String problemType = blankToNull(query.getProblemType());
        String categoryNameEn = blankToNull(query.getCategoryNameEn());
        String keyword = blankToNull(query.getKeyword());
        LambdaQueryWrapper<BizProblemSolution> wrapper = new LambdaQueryWrapper<BizProblemSolution>()
                .eq(problemType != null, BizProblemSolution::getProblemType, problemType)
                .eq(categoryNameEn != null, BizProblemSolution::getCategoryNameEn, categoryNameEn)
                .eq(query.getEnabled() != null, BizProblemSolution::getEnabled, query.getEnabled())
                .and(keyword != null, condition -> condition
                        .like(BizProblemSolution::getSolutionTitle, keyword)
                        .or()
                        .like(BizProblemSolution::getSolutionContent, keyword)
                        .or()
                        .like(BizProblemSolution::getKeywords, keyword))
                .orderByDesc(BizProblemSolution::getPriority)
                .orderByDesc(BizProblemSolution::getUpdateTime);
        Page<BizProblemSolution> page = problemSolutionMapper.selectPage(new Page<>(normalizePageNum(query.getPageNum()),
                normalizePageSize(query.getPageSize())), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public List<ProblemSolutionVO> recommendSolutions(String problemType, String categoryNameEn, String keyword) {
        String cleanProblemType = blankToNull(problemType);
        String cleanCategoryNameEn = blankToNull(categoryNameEn);
        String cleanKeyword = blankToNull(keyword);
        LambdaQueryWrapper<BizProblemSolution> wrapper = new LambdaQueryWrapper<BizProblemSolution>()
                .eq(BizProblemSolution::getEnabled, 1)
                .eq(cleanProblemType != null, BizProblemSolution::getProblemType, cleanProblemType)
                .and(cleanCategoryNameEn != null, condition -> condition
                        .eq(BizProblemSolution::getCategoryNameEn, cleanCategoryNameEn)
                        .or()
                        .isNull(BizProblemSolution::getCategoryNameEn))
                .and(cleanKeyword != null, condition -> condition
                        .like(BizProblemSolution::getSolutionTitle, cleanKeyword)
                        .or()
                        .like(BizProblemSolution::getSolutionContent, cleanKeyword)
                        .or()
                        .like(BizProblemSolution::getKeywords, cleanKeyword))
                .orderByDesc(BizProblemSolution::getPriority)
                .orderByDesc(BizProblemSolution::getUseCount)
                .last("limit 5");
        return problemSolutionMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ProblemSolutionVO createSolution(ProblemSolutionDTO problemSolutionDTO) {
        validate(problemSolutionDTO);
        BizProblemSolution solution = new BizProblemSolution();
        copyToEntity(problemSolutionDTO, solution);
        solution.setUseCount(0);
        solution.setCreateTime(LocalDateTime.now());
        solution.setUpdateTime(LocalDateTime.now());
        problemSolutionMapper.insert(solution);
        return toVO(solution);
    }

    @Override
    public ProblemSolutionVO updateSolution(Long solutionId, ProblemSolutionDTO problemSolutionDTO) {
        validate(problemSolutionDTO);
        BizProblemSolution solution = requireSolution(solutionId);
        copyToEntity(problemSolutionDTO, solution);
        solution.setUpdateTime(LocalDateTime.now());
        problemSolutionMapper.updateById(solution);
        return toVO(solution);
    }

    @Override
    public ProblemSolutionVO updateStatus(Long solutionId, Integer enabled) {
        BizProblemSolution solution = requireSolution(solutionId);
        solution.setEnabled(normalizeEnabled(enabled));
        solution.setUpdateTime(LocalDateTime.now());
        problemSolutionMapper.updateById(solution);
        return toVO(solution);
    }

    private void validate(ProblemSolutionDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "解决方案参数不能为空");
        }
        if (blankToNull(dto.getProblemType()) == null) {
            throw new BusinessException(400, "问题类型不能为空");
        }
        if (blankToNull(dto.getSolutionTitle()) == null) {
            throw new BusinessException(400, "方案标题不能为空");
        }
        if (blankToNull(dto.getSolutionContent()) == null) {
            throw new BusinessException(400, "方案内容不能为空");
        }
    }

    private void copyToEntity(ProblemSolutionDTO dto, BizProblemSolution solution) {
        solution.setProblemType(blankToNull(dto.getProblemType()));
        solution.setCategoryNameEn(blankToNull(dto.getCategoryNameEn()));
        solution.setSolutionTitle(blankToNull(dto.getSolutionTitle()));
        solution.setSolutionContent(blankToNull(dto.getSolutionContent()));
        solution.setKeywords(blankToNull(dto.getKeywords()));
        solution.setSourceType(blankToNull(dto.getSourceType()) == null ? "manual" : blankToNull(dto.getSourceType()));
        solution.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        solution.setEnabled(normalizeEnabled(dto.getEnabled()));
    }

    private BizProblemSolution requireSolution(Long solutionId) {
        BizProblemSolution solution = problemSolutionMapper.selectById(solutionId);
        if (solution == null) {
            throw new BusinessException(404, "解决方案不存在");
        }
        return solution;
    }

    private ProblemSolutionVO toVO(BizProblemSolution solution) {
        return new ProblemSolutionVO(solution.getId(), solution.getProblemType(), solution.getCategoryNameEn(),
                solution.getSolutionTitle(), solution.getSolutionContent(), solution.getKeywords(),
                solution.getSourceType(), solution.getPriority(), solution.getUseCount(), solution.getEnabled(),
                solution.getCreateTime(), solution.getUpdateTime());
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private int normalizeEnabled(Integer enabled) {
        return Integer.valueOf(0).equals(enabled) ? 0 : 1;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
