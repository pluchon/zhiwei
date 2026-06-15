package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.AssetCategoryDTO;
import com.campus.system.dto.AssetCategoryQueryDTO;
import com.campus.system.vo.AssetCategoryVO;
import java.util.List;

// 资产分类业务接口
public interface AssetCategoryService {

    List<AssetCategoryVO> enabledList();

    PageResult<AssetCategoryVO> adminList(int pageNum, int pageSize, AssetCategoryQueryDTO query);

    List<AssetCategoryVO> listForExportByIds(List<Long> assetCategoryIds);

    List<AssetCategoryVO> listForExport(AssetCategoryQueryDTO query);

    AssetCategoryVO create(AssetCategoryDTO body);

    AssetCategoryVO update(Long id, AssetCategoryDTO body);

    void disable(Long id);
}
