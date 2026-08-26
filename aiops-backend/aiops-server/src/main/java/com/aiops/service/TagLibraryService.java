package com.aiops.service;

import com.aiops.dto.CustomTagDTO;
import com.aiops.dto.CustomTagQueryDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.CustomTagVO;

import java.util.List;

public interface TagLibraryService {
    PageResult<CustomTagVO> pageTags(CustomTagQueryDTO queryDTO);

    List<CustomTagVO> activeTags();

    CustomTagVO createTag(CustomTagDTO customTagDTO);

    CustomTagVO updateTag(Long tagId, CustomTagDTO customTagDTO);

    CustomTagVO updateStatus(Long tagId, Integer enabled);
}
