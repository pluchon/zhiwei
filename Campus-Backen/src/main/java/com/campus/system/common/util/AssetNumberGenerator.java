package com.campus.system.common.util;

import com.campus.system.mapper.AssetMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// 资产编号生成器，格式 AST-yyyyMM-六位序号
@Component
public class AssetNumberGenerator {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private AssetMapper assetMapper;

    public String nextAssetNo(LocalDate date) {
        String prefix = "AST-" + date.format(MONTH_FORMAT) + "-";
        String latestNo = assetMapper.findLatestAssetNoIncludingDeleted(prefix);
        int nextSeq = 1;
        if (latestNo != null) {
            String suffix = latestNo.substring(prefix.length());
            nextSeq = Integer.parseInt(suffix) + 1;
        }
        if (nextSeq > 999999) {
            throw new IllegalStateException("当月资产编号序号已用尽");
        }
        return prefix + String.format("%06d", nextSeq);
    }
}
