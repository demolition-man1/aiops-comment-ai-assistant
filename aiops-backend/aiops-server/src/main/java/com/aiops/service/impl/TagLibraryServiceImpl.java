package com.aiops.service.impl;

import com.aiops.dto.CustomTagDTO;
import com.aiops.dto.CustomTagQueryDTO;
import com.aiops.entity.BizCustomTag;
import com.aiops.exception.BusinessException;
import com.aiops.mapper.BizCustomTagMapper;
import com.aiops.result.PageResult;
import com.aiops.service.TagLibraryService;
import com.aiops.vo.CustomTagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagLibraryServiceImpl implements TagLibraryService {

    private final BizCustomTagMapper customTagMapper;

    @Override
    public PageResult<CustomTagVO> pageTags(CustomTagQueryDTO queryDTO) {
        CustomTagQueryDTO query = queryDTO == null ? new CustomTagQueryDTO() : queryDTO;
        String keyword = blankToNull(query.getKeyword());
        String tagGroup = blankToNull(query.getTagGroup());
        LambdaQueryWrapper<BizCustomTag> wrapper = new LambdaQueryWrapper<BizCustomTag>()
                .eq(tagGroup != null, BizCustomTag::getTagGroup, tagGroup)
                .eq(query.getEnabled() != null, BizCustomTag::getEnabled, query.getEnabled())
                .and(keyword != null, condition -> condition
                        .like(BizCustomTag::getTagName, keyword)
                        .or()
                        .like(BizCustomTag::getDescription, keyword))
                .orderByDesc(BizCustomTag::getSortOrder)
                .orderByDesc(BizCustomTag::getUpdateTime);
        Page<BizCustomTag> page = customTagMapper.selectPage(new Page<>(normalizePageNum(query.getPageNum()),
                normalizePageSize(query.getPageSize())), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public List<CustomTagVO> activeTags() {
        return customTagMapper.selectList(new LambdaQueryWrapper<BizCustomTag>()
                        .eq(BizCustomTag::getEnabled, 1)
                        .orderByDesc(BizCustomTag::getSortOrder)
                        .orderByDesc(BizCustomTag::getUpdateTime))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public CustomTagVO createTag(CustomTagDTO customTagDTO) {
        validate(customTagDTO);
        BizCustomTag tag = new BizCustomTag();
        copyToEntity(customTagDTO, tag);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        customTagMapper.insert(tag);
        return toVO(tag);
    }

    @Override
    public CustomTagVO updateTag(Long tagId, CustomTagDTO customTagDTO) {
        validate(customTagDTO);
        BizCustomTag tag = requireTag(tagId);
        copyToEntity(customTagDTO, tag);
        tag.setUpdateTime(LocalDateTime.now());
        customTagMapper.updateById(tag);
        return toVO(tag);
    }

    @Override
    public CustomTagVO updateStatus(Long tagId, Integer enabled) {
        BizCustomTag tag = requireTag(tagId);
        tag.setEnabled(normalizeEnabled(enabled));
        tag.setUpdateTime(LocalDateTime.now());
        customTagMapper.updateById(tag);
        return toVO(tag);
    }

    private void validate(CustomTagDTO dto) {
        if (dto == null) {
            throw new BusinessException(400, "标签参数不能为空");
        }
        if (blankToNull(dto.getTagName()) == null) {
            throw new BusinessException(400, "标签名称不能为空");
        }
    }

    private void copyToEntity(CustomTagDTO dto, BizCustomTag tag) {
        tag.setTagName(blankToNull(dto.getTagName()));
        tag.setTagGroup(blankToNull(dto.getTagGroup()));
        tag.setColor(blankToNull(dto.getColor()));
        tag.setDescription(blankToNull(dto.getDescription()));
        tag.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        tag.setEnabled(normalizeEnabled(dto.getEnabled()));
    }

    private BizCustomTag requireTag(Long tagId) {
        BizCustomTag tag = customTagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        return tag;
    }

    private CustomTagVO toVO(BizCustomTag tag) {
        return new CustomTagVO(tag.getId(), tag.getTagName(), tag.getTagGroup(), tag.getColor(),
                tag.getDescription(), tag.getSortOrder(), tag.getEnabled(), tag.getCreateTime(), tag.getUpdateTime());
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
