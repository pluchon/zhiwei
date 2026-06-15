package com.campus.system.service.interfaces;

import com.campus.system.common.result.PageResult;
import com.campus.system.dto.AdminArbitrateDTO;
import com.campus.system.dto.AdminCreateUserDTO;
import com.campus.system.dto.AdminOrderActionDTO;
import com.campus.system.dto.AdminUpdateUserDTO;
import com.campus.system.dto.RepairCapabilityDTO;
import com.campus.system.dto.RepairCategoryDTO;
import com.campus.system.dto.SysDictDataDTO;
import com.campus.system.dto.UserQueryDTO;
import com.campus.system.vo.RepairCapabilityVO;
import com.campus.system.vo.RepairCategoryVO;
import com.campus.system.vo.SysDictDataVO;
import com.campus.system.vo.SysDictTypeVO;
import com.campus.system.vo.SysLoginLogVO;
import com.campus.system.vo.SysOperationLogVO;
import com.campus.system.vo.UserVO;
import java.util.List;

// 管理后台业务接口。
public interface AdminService {

    PageResult<UserVO> users(int pageNum, int pageSize, UserQueryDTO query);

    List<UserVO> listUsersForExportByIds(List<Long> userIds);

    List<UserVO> listUsersForExport(UserQueryDTO query);

    UserVO createUser(AdminCreateUserDTO body);

    UserVO updateUser(Long id, AdminUpdateUserDTO body);

    List<RepairCategoryVO> categories();

    RepairCategoryVO createCategory(RepairCategoryDTO category);

    RepairCategoryVO updateCategory(Long id, RepairCategoryDTO category);

    List<RepairCapabilityVO> capabilities();

    RepairCapabilityVO createCapability(RepairCapabilityDTO capability);

    void deleteCapability(Long id);

    List<SysDictTypeVO> dictTypes();

    List<SysDictDataVO> dictData(String dictType);

    SysDictDataVO createDictData(SysDictDataDTO data);

    SysDictDataVO updateDictData(Long id, SysDictDataDTO data);

    PageResult<SysLoginLogVO> loginLogs(int pageNum, int pageSize);

    PageResult<SysOperationLogVO> operationLogs(int pageNum, int pageSize);

    void rejectOrder(Long id, AdminOrderActionDTO body);

    void closeOrder(Long id, AdminOrderActionDTO body);

    void arbitrateOrder(Long id, AdminArbitrateDTO body);
}
