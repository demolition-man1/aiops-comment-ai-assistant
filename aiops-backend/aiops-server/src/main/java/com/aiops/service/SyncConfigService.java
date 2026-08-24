package com.aiops.service;

import com.aiops.dto.SyncConfigDTO;
import com.aiops.result.PageResult;
import com.aiops.vo.SyncConfigVO;
import com.aiops.vo.SyncExecutionVO;

public interface SyncConfigService {
    PageResult<SyncConfigVO> pageConfigs(Integer pageNum, Integer pageSize, String sourceType, Integer enabled);

    SyncConfigVO createConfig(SyncConfigDTO syncConfigDTO);

    SyncConfigVO updateConfig(Long configId, SyncConfigDTO syncConfigDTO);

    SyncConfigVO enableConfig(Long configId);

    SyncConfigVO disableConfig(Long configId);

    SyncExecutionVO triggerNow(Long configId);

    SyncExecutionVO executeSyncConfig(Long configId, String triggerType);

    PageResult<SyncExecutionVO> pageExecutions(Integer pageNum, Integer pageSize, Long configId, String status);
}
