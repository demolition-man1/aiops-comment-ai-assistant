package com.aiops.service;

import com.aiops.dto.AiContentGenerateDTO;
import com.aiops.dto.AiReportGenerateDTO;
import com.aiops.dto.CommentTranslateDTO;
import com.aiops.dto.NegativeReplyEffectDTO;
import com.aiops.dto.NegativeReplyFavoriteDTO;
import com.aiops.dto.NegativeReplyGenerateDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.AiContentVO;
import com.aiops.vo.CommentTranslationVO;
import com.aiops.vo.NegativeReplyVO;
import com.aiops.vo.OperationReportVO;

public interface AiService {
    OperationReportVO generateProductReport(AiReportGenerateDTO generateDTO);

    OperationReportVO generateSellerReport(AiReportGenerateDTO generateDTO);

    PageResult<OperationReportVO> pageReports(String targetType, String targetId, Integer pageNum, Integer pageSize);

    OperationReportVO getReport(Long reportId);

    AiContentVO generateContent(AiContentGenerateDTO generateDTO);

    PageResult<AiContentVO> pageContents(String targetType, String targetId, String contentType, Integer pageNum, Integer pageSize);

    NegativeReplyVO generateNegativeReply(NegativeReplyGenerateDTO generateDTO);

    CommentTranslationVO translateComment(Long commentId, CommentTranslateDTO translateDTO);

    PageResult<NegativeReplyVO> pageNegativeReplies(String productId, String sellerId, Integer pageNum, Integer pageSize);

    NegativeReplyVO markNegativeReplyUsed(Long replyId);

    NegativeReplyVO updateNegativeReplyEffect(Long replyId, NegativeReplyEffectDTO effectDTO);

    NegativeReplyVO updateNegativeReplyFavorite(Long replyId, NegativeReplyFavoriteDTO favoriteDTO);
}
