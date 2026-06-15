package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.AssetEditDTO;
import com.campus.system.dto.AssetQueryDTO;
import com.campus.system.dto.AssetStatusChangeDTO;
import com.campus.system.entity.Asset;
import com.campus.system.entity.RepairOrder;
import com.campus.system.vo.AssetRepairHistoryVO;
import com.campus.system.vo.AssetVO;
import org.springframework.web.multipart.MultipartFile;

// 资产台账业务接口
public interface AssetService {

    PageResult<AssetVO> search(int pageNum, int pageSize, AssetQueryDTO query);

    AssetVO detail(Long id);

    AssetVO create(AssetEditDTO body);

    AssetVO update(Long id, AssetEditDTO body);

    void changeStatus(Long id, AssetStatusChangeDTO body);

    void delete(Long id, Integer version);

    void restore(Long id);

    PageResult<AssetRepairHistoryVO> repairHistory(Long assetId, int pageNum, int pageSize);

    String uploadImage(MultipartFile file) throws Exception;

    void applyAssetDraft(RepairOrder order, String repairType, Long assetId);

    void claimAssetOnSubmit(RepairOrder order);

    void onOrderAccepted(RepairOrder order);

    void onOrderEnded(RepairOrder order);

    void releaseOnWithdrawToDraft(RepairOrder order);

    void repairStaleActiveOrderLink(Long assetId);

    Asset requireAssetForRepair(Long assetId);
}
