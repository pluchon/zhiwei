package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.AssetImportBatchQueryDTO;
import com.campus.system.dto.AssetImportConfirmDTO;
import com.campus.system.dto.AssetImportItemUpdateDTO;
import com.campus.system.vo.AssetImportBatchVO;
import com.campus.system.vo.AssetImportConfirmResultVO;
import com.campus.system.vo.AssetImportItemVO;
import org.springframework.web.multipart.MultipartFile;

// 资产批量导入业务接口
public interface AssetImportService {

    AssetImportBatchVO upload(MultipartFile file);

    AssetImportBatchVO uploadImages(MultipartFile[] files);

    PageResult<AssetImportBatchVO> listBatches(int pageNum, int pageSize, AssetImportBatchQueryDTO query);

    AssetImportBatchVO getBatch(Long batchId);

    PageResult<AssetImportItemVO> listBatchItems(Long batchId, String status, int pageNum, int pageSize);

    AssetImportItemVO updateItem(Long itemId, AssetImportItemUpdateDTO body);

    void ignoreItem(Long itemId);

    AssetImportConfirmResultVO confirmItem(Long itemId);

    AssetImportConfirmResultVO confirmBatch(AssetImportConfirmDTO body);

    void deleteBatch(Long batchId);
}
