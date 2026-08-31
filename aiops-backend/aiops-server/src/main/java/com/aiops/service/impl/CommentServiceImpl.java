package com.aiops.service.impl;

import com.aiops.constant.RedisKeyConstant;
import com.aiops.converter.AnalysisJsonConverter;
import com.aiops.dto.CommentQueryDTO;
import com.aiops.dto.CommentTagUpdateDTO;
import com.aiops.entity.BizComment;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizCommentMapper;
import com.aiops.result.PageResult;
import com.aiops.service.CacheService;
import com.aiops.service.CommentService;
import com.aiops.vo.CommentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final List<String> REPORT_CACHE_LANGUAGES = List.of("zh-CN", "en-US", "pt-BR");

    private final BizCommentMapper commentMapper;
    private final AnalysisJsonConverter analysisJsonConverter;
    private final CacheService cacheService;

    @Override
    public PageResult<CommentVO> pageComments(CommentQueryDTO queryDTO) {
        Page<BizComment> page = commentMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), buildWrapper(queryDTO));
        List<CommentVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    public CommentVO getComment(Long commentId) {
        BizComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        return toVO(comment);
    }

    @Override
    public PageResult<CommentVO> pageNegativeComments(CommentQueryDTO queryDTO) {
        queryDTO.setSentiment("negative");
        Page<BizComment> page = commentMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), buildWrapper(queryDTO));
        List<CommentVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    public CommentVO updateTags(Long commentId, CommentTagUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new BusinessException(400, "评论标签参数不能为空");
        }
        BizComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        comment.setManualProblemType(blankToNull(updateDTO.getManualProblemType()));
        comment.setCustomTags(analysisJsonConverter.toJsonArray(updateDTO.getCustomTags()));
        comment.setTagUpdateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
        evictAnalysisCache(comment);
        return toVO(comment);
    }

    private LambdaQueryWrapper<BizComment> buildWrapper(CommentQueryDTO queryDTO) {
        String productId = blankToNull(queryDTO.getProductId());
        String sellerId = blankToNull(queryDTO.getSellerId());
        LambdaQueryWrapper<BizComment> wrapper = new LambdaQueryWrapper<BizComment>()
                .eq(queryDTO.getCommentId() != null, BizComment::getId, queryDTO.getCommentId())
                .eq(blankToNull(queryDTO.getSentiment()) != null, BizComment::getSentiment, blankToNull(queryDTO.getSentiment()))
                .eq(queryDTO.getIsNegative() != null, BizComment::getIsNegative, queryDTO.getIsNegative())
                .ge(queryDTO.getMinScore() != null, BizComment::getReviewScore, queryDTO.getMinScore())
                .le(queryDTO.getMaxScore() != null, BizComment::getReviewScore, queryDTO.getMaxScore())
                .orderByDesc(BizComment::getReviewTime);
        applyIdFilter(wrapper, BizComment::getProductId, productId);
        applyIdFilter(wrapper, BizComment::getSellerId, sellerId);
        String problemType = blankToNull(queryDTO.getProblemType());
        if (problemType != null) {
            wrapper.and(condition -> condition
                    .eq(BizComment::getManualProblemType, problemType)
                    .or(subCondition -> subCondition
                            .isNull(BizComment::getManualProblemType)
                            .eq(BizComment::getProblemType, problemType)));
        }
        return wrapper;
    }

    private void applyIdFilter(LambdaQueryWrapper<BizComment> wrapper,
                               SFunction<BizComment, String> column,
                               String value) {
        if (value == null) {
            return;
        }
        if (value.length() >= 32) {
            wrapper.eq(column, value);
            return;
        }
        wrapper.likeRight(column, value);
    }

    private CommentVO toVO(BizComment comment) {
        return new CommentVO(comment.getId(), comment.getReviewId(), comment.getProductId(), comment.getSellerId(),
                comment.getReviewScore(), comment.getReviewTitle(), comment.getReviewContent(), comment.getCleanContent(),
                comment.getSentiment(), comment.getProblemType(), comment.getManualProblemType(),
                effectiveProblemType(comment), analysisJsonConverter.parseStringList(comment.getCustomTags()),
                comment.getIsNegative(), comment.getReviewTime());
    }

    private String effectiveProblemType(BizComment comment) {
        String manualProblemType = blankToNull(comment.getManualProblemType());
        return manualProblemType == null ? comment.getProblemType() : manualProblemType;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void evictAnalysisCache(BizComment comment) {
        if (comment.getProductId() != null) {
            cacheService.delete(String.format(RedisKeyConstant.ANALYSIS_PRODUCT, comment.getProductId()));
            evictReportCache("product", comment.getProductId());
        }
        if (comment.getSellerId() != null) {
            cacheService.delete(String.format(RedisKeyConstant.ANALYSIS_SELLER, comment.getSellerId()));
            evictReportCache("seller", comment.getSellerId());
        }
    }

    private void evictReportCache(String targetType, String targetId) {
        for (String language : REPORT_CACHE_LANGUAGES) {
            cacheService.delete(String.format(RedisKeyConstant.AI_REPORT, targetType, targetId, language));
        }
        cacheService.delete("ai:report:" + targetType + ":" + targetId);
    }
}
