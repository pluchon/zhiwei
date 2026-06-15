package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.ManualRecoveryCreateDTO;
import com.campus.system.dto.ManualRecoveryPhoneVerifyDTO;
import com.campus.system.dto.ManualRecoveryQueryDTO;
import com.campus.system.dto.ManualRecoveryReviewDTO;
import com.campus.system.vo.ManualRecoveryVO;

// 账号人工恢复业务接口
public interface ManualAccountRecoveryService {

    ManualRecoveryVO create(ManualRecoveryCreateDTO body);

    void cancel(Long recoveryId);

    ManualRecoveryVO review(Long recoveryId, ManualRecoveryReviewDTO body);

    void verifyPhone(Long recoveryId, ManualRecoveryPhoneVerifyDTO body);

    PageResult<ManualRecoveryVO> search(int pageNum, int pageSize, ManualRecoveryQueryDTO query);

    ManualRecoveryVO detail(Long recoveryId);

    ManualRecoveryVO verifyInfo(Long recoveryId);

    void expireApprovedRecords();
}
