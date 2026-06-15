package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.RepairerSuggestionHandleDTO;
import com.campus.system.dto.RepairerSuggestionQueryDTO;
import com.campus.system.dto.RepairerSuggestionSubmitDTO;
import com.campus.system.vo.RepairerSuggestionVO;
import com.campus.system.vo.SuggestionSimilarityVO;

// 维修师傅建议业务接口
public interface RepairerSuggestionService {

    PageResult<RepairerSuggestionVO> myList(int pageNum, int pageSize, RepairerSuggestionQueryDTO query);

    PageResult<RepairerSuggestionVO> adminList(int pageNum, int pageSize, RepairerSuggestionQueryDTO query);

    RepairerSuggestionVO submit(RepairerSuggestionSubmitDTO body);

    SuggestionSimilarityVO checkSimilarity(RepairerSuggestionSubmitDTO body, Long excludeSuggestionId);

    RepairerSuggestionVO update(Long id, RepairerSuggestionSubmitDTO body);

    void withdraw(Long id);

    void handle(Long id, RepairerSuggestionHandleDTO body);

    RepairerSuggestionVO detail(Long id);
}
