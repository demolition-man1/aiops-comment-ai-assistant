package com.aiops.service;

import com.aiops.result.PageResult;
import com.aiops.vo.TaskRecordVO;
import com.aiops.vo.TaskVO;

public interface TaskCenterService {
    PageResult<TaskRecordVO> pageTasks(Integer pageNum, Integer pageSize, String taskType, String taskStatus, String keyword);

    TaskRecordVO getTask(String recordKey);

    TaskVO retryTask(String recordKey);
}
