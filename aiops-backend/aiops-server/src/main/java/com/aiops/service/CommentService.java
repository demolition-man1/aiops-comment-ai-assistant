package com.aiops.service;

import com.aiops.dto.CommentQueryDTO;
import com.aiops.dto.CommentTagUpdateDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.CommentVO;

public interface CommentService {
    PageResult<CommentVO> pageComments(CommentQueryDTO queryDTO);

    CommentVO getComment(Long commentId);

    PageResult<CommentVO> pageNegativeComments(CommentQueryDTO queryDTO);

    CommentVO updateTags(Long commentId, CommentTagUpdateDTO updateDTO);
}
