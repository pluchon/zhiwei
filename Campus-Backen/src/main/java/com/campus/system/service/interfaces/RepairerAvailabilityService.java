package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.RepairerAvailabilityUpdateDTO;
import com.campus.system.vo.RepairerAvailabilityVO;

// 维修师傅接单状态业务接口
public interface RepairerAvailabilityService {

    RepairerAvailabilityVO mine();

    RepairerAvailabilityVO updateMine(RepairerAvailabilityUpdateDTO body);

    PageResult<RepairerAvailabilityVO> listForAdmin(int pageNum, int pageSize, String acceptingState);
}
