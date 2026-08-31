package com.aiops.service;

import com.aiops.vo.RagIndexStatusVO;

public interface RagKnowledgeService {

    RagIndexStatusVO getStatus();

    RagIndexStatusVO reindex();
}
