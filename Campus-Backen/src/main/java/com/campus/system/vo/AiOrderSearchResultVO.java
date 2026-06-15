package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 工单语义搜索结果
@Data
public class AiOrderSearchResultVO {

    // 结果摘要
    private String summary;

    // 搜索结果
    private List<AiOrderSearchItemVO> items;
}
