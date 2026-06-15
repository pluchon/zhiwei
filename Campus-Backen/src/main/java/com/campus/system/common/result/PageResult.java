package com.campus.system.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

// 分页结果返回
public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize) {

    // 记录数，总计，第几页，该页的记录个数
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

}