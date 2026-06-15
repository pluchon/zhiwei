package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.converter.EntityVOConverter;
import com.campus.system.dto.AdminArbitrateDTO;
import com.campus.system.dto.AdminCreateUserDTO;
import com.campus.system.dto.AdminOrderActionDTO;
import com.campus.system.dto.AdminUpdateUserDTO;
import com.campus.system.dto.RepairCapabilityDTO;
import com.campus.system.dto.RepairCategoryDTO;
import com.campus.system.dto.SysDictDataDTO;
import com.campus.system.dto.UserQueryDTO;
import com.campus.system.entity.RepairCapability;
import com.campus.system.entity.RepairCategory;
import com.campus.system.entity.SysDictData;
import com.campus.system.entity.SysLoginLog;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.RepairCapabilityMapper;
import com.campus.system.mapper.RepairCategoryMapper;
import com.campus.system.mapper.SysDictDataMapper;
import com.campus.system.mapper.SysDictTypeMapper;
import com.campus.system.mapper.SysLoginLogMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import com.campus.system.service.interfaces.AdminService;
import com.campus.system.service.interfaces.AuthService;
import com.campus.system.service.interfaces.RepairOrderService;
import com.campus.system.vo.RepairCapabilityVO;
import com.campus.system.vo.RepairCategoryVO;
import com.campus.system.vo.SysDictDataVO;
import com.campus.system.vo.SysDictTypeVO;
import com.campus.system.vo.SysLoginLogVO;
import com.campus.system.vo.SysOperationLogVO;
import com.campus.system.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 管理后台
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    @Autowired
    private RepairCategoryMapper categories;

    @Autowired
    private RepairCapabilityMapper capabilities;

    @Autowired
    private SysDictTypeMapper dictTypes;

    @Autowired
    private SysDictDataMapper dictData;

    @Autowired
    private SysLoginLogMapper loginLogs;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Autowired
    private RepairOrderService repairOrderService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthService auth;

    // 分页查询平台所有用户列表
    @Override
    public PageResult<UserVO> users(int pageNum, int pageSize, UserQueryDTO query) {
        UserQueryDTO safeQuery = query == null ? new UserQueryDTO() : query;
        IPage<SysUser> page = users.selectPage(Page.of(pageNum, pageSize), buildUserQueryWrapper(safeQuery));
        return new PageResult<>(EntityVOConverter.toUserVOList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<UserVO> listUsersForExportByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw BusinessException.badRequest("请先选择要导出的用户");
        }
        if (userIds.size() > 1000) {
            throw BusinessException.badRequest("单次最多导出 1000 条");
        }
        List<SysUser> list = users.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getUserId, userIds));
        if (list.size() != userIds.size()) {
            throw BusinessException.badRequest("部分用户不存在");
        }
        Map<Long, SysUser> userMap = list.stream()
                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> left));
        List<SysUser> ordered = userIds.stream().map(userMap::get).filter(Objects::nonNull).toList();
        return EntityVOConverter.toUserVOList(ordered);
    }

    @Override
    public List<UserVO> listUsersForExport(UserQueryDTO query) {
        UserQueryDTO safeQuery = query == null ? new UserQueryDTO() : query;
        IPage<SysUser> page = users.selectPage(Page.of(1, 1001), buildUserQueryWrapper(safeQuery));
        if (page.getTotal() > 1000) {
            throw BusinessException.badRequest("导出数量超过 1000 条，请缩小筛选范围");
        }
        return EntityVOConverter.toUserVOList(page.getRecords());
    }

    private LambdaQueryWrapper<SysUser> buildUserQueryWrapper(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(SysUser::getRealName, query.getKeyword().trim());
        }
        List<Long> roleIds = parseLongCsv(query.getRoleIds());
        if (!roleIds.isEmpty()) {
            wrapper.in(SysUser::getRoleId, roleIds);
        }
        List<Integer> activationStatuses = parseIntegerCsv(query.getActivationStatuses());
        if (!activationStatuses.isEmpty()) {
            wrapper.in(SysUser::getActivationStatus, activationStatuses);
        }
        List<Integer> accountStatuses = parseIntegerCsv(query.getAccountStatuses());
        if (!accountStatuses.isEmpty()) {
            wrapper.in(SysUser::getAccountStatus, accountStatuses);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return wrapper;
    }

    private List<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(part -> !part.isEmpty()).map(Long::valueOf).distinct().toList();
    }

    private List<Integer> parseIntegerCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(part -> !part.isEmpty()).map(Integer::valueOf).distinct().toList();
    }

    /**
     * 管理员创建用户。
     * 事务边界覆盖用户写入和操作日志写入；禁止创建管理员账号，避免后台普通管理员越权扩权。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(AdminCreateUserDTO body) {
        String role = required(body.getRoleCode(), "roleCode");
        if ("ADMIN".equals(role)) {
            throw BusinessException.forbidden("不能创建管理员");
        }
        SysRole roleEntity = requireRole(role);
        SysUser user = new SysUser();
        user.setUserNo(required(body.getUserNo(), "userNo"));
        user.setRealName(required(body.getRealName(), "realName"));
        user.setNickName(required(body.getNickName(), "nickName"));
        user.setRoleId(roleEntity.getRoleId());
        user.setPhoneNumber(required(body.getPhoneNumber(), "phoneNumber"));
        user.setParentPhone(body.getParentPhone());
        user.setPassword(encoder.encode(required(body.getInitialPassword(), "initialPassword")));
        user.setActivationStatus("REPAIRER".equals(role) ? 1 : 0);
        user.setAccountStatus(0);
        user.setSecurityStamp(random());
        user.setPhoneConfirmRequired(0);
        users.insert(user);
        audit("CREATE_USER", "USER", user.getUserId(), "创建用户 " + user.getUserNo());
        return EntityVOConverter.toUserVO(user);
    }

    /**
     * 管理员编辑用户。
     * 修改角色、状态或联系方式后刷新 securityStamp 并清理 Redis 会话，保证旧令牌立即失效。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, AdminUpdateUserDTO body) {
        SysUser user = requireUser(id);
        SysRole oldRole = roles.selectById(user.getRoleId());
        if (oldRole != null && "ADMIN".equals(oldRole.getRoleName())) {
            throw BusinessException.forbidden("不能修改管理员");
        }
        if (body.getRoleCode() != null) {
            if ("ADMIN".equals(body.getRoleCode())) {
                throw BusinessException.forbidden("不能提升为管理员");
            }
            user.setRoleId(requireRole(body.getRoleCode()).getRoleId());
        }
        if (body.getAccountStatus() != null) {
            user.setAccountStatus(body.getAccountStatus());
        }
        if (body.getNickName() != null) {
            user.setNickName(body.getNickName());
        }
        if (body.getPhoneNumber() != null) {
            user.setPhoneNumber(body.getPhoneNumber());
        }
        user.setSecurityStamp(random());
        users.updateById(user);
        auth.clearSessions(id);
        audit("UPDATE_USER", "USER", id, "更新用户状态或资料");
        return EntityVOConverter.toUserVO(user);
    }

    /**
     * 查询所有维修故障分类
     */
    @Override
    public List<RepairCategoryVO> categories() {
        return EntityVOConverter.toRepairCategoryVOList(categories.selectList(Wrappers.lambdaQuery()));
    }

    /**
     * 新增一个维修故障分类
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairCategoryVO createCategory(RepairCategoryDTO body) {
        RepairCategory category = new RepairCategory();
        category.setCategoryName(required(body.getCategoryName(), "categoryName"));
        category.setDescription(body.getDescription());
        category.setStatus(defaultZero(body.getStatus()));
        categories.insert(category);
        audit("CREATE_CATEGORY", "CATEGORY", category.getCategoryId(), "创建故障类型");
        return EntityVOConverter.toRepairCategoryVO(category);
    }

    /**
     * 更新维修故障分类信息或状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairCategoryVO updateCategory(Long id, RepairCategoryDTO body) {
        RepairCategory category = new RepairCategory();
        category.setCategoryId(id);
        category.setCategoryName(body.getCategoryName());
        category.setDescription(body.getDescription());
        category.setStatus(body.getStatus());
        categories.updateById(category);
        audit("UPDATE_CATEGORY", "CATEGORY", id, "更新故障类型");
        return EntityVOConverter.toRepairCategoryVO(categories.selectById(id));
    }

    /**
     * 查询所有维修师傅的维修能力列表
     */
    @Override
    public List<RepairCapabilityVO> capabilities() {
        return EntityVOConverter.toRepairCapabilityVOList(capabilities.selectList(Wrappers.lambdaQuery()));
    }

    /**
     * 为维修师傅分配维修能力。
     * 能力配置会影响待接单大厅的动态筛选，因此写入后立即生效，不做缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairCapabilityVO createCapability(RepairCapabilityDTO body) {
        RepairCapability capability = new RepairCapability();
        capability.setRepairerId(required(body.getRepairerId(), "repairerId"));
        capability.setCategoryId(required(body.getCategoryId(), "categoryId"));
        capabilities.insert(capability);
        audit("ADD_CAPABILITY", "USER", capability.getRepairerId(), "新增维修能力");
        return EntityVOConverter.toRepairCapabilityVO(capability);
    }

    /**
     * 移除维修师傅的某项维修能力
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCapability(Long id) {
        RepairCapability capability = capabilities.selectById(id);
        if (capability == null) {
            throw BusinessException.notFound("维修能力不存在");
        }
        capabilities.deleteById(id);
        audit("REMOVE_CAPABILITY", "USER", capability.getRepairerId(), "移除维修能力");
    }

    /**
     * 查询所有数据字典类型
     */
    @Override
    public List<SysDictTypeVO> dictTypes() {
        return EntityVOConverter.toSysDictTypeVOList(dictTypes.selectList(Wrappers.lambdaQuery()));
    }

    /**
     * 根据字典类型查询其包含的数据字典项列表
     */
    @Override
    public List<SysDictDataVO> dictData(String dictType) {
        return EntityVOConverter.toSysDictDataVOList(dictData.selectList(Wrappers.<SysDictData>lambdaQuery().eq(dictType != null, SysDictData::getDictType, dictType)));
    }

    /**
     * 新增一条字典数据项
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictDataVO createDictData(SysDictDataDTO body) {
        SysDictData data = new SysDictData();
        applyDictData(data, body);
        dictData.insert(data);
        audit("CREATE_DICT_DATA", "DICT", data.getDictDataId(), "创建字典数据");
        return EntityVOConverter.toSysDictDataVO(data);
    }

    /**
     * 更新字典数据项的信息或状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictDataVO updateDictData(Long id, SysDictDataDTO body) {
        SysDictData data = new SysDictData();
        data.setDictDataId(id);
        applyDictData(data, body);
        dictData.updateById(data);
        audit("UPDATE_DICT_DATA", "DICT", id, "更新字典数据");
        return EntityVOConverter.toSysDictDataVO(dictData.selectById(id));
    }

    /**
     * 分页查询后台登录日志
     */
    @Override
    public PageResult<SysLoginLogVO> loginLogs(int pageNum, int pageSize) {
        IPage<SysLoginLog> page = loginLogs.selectPage(Page.of(pageNum, pageSize), Wrappers.<SysLoginLog>lambdaQuery().orderByDesc(SysLoginLog::getCreateTime));
        return new PageResult<>(EntityVOConverter.toSysLoginLogVOList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 分页查询后台敏感操作审计日志
     */
    @Override
    public PageResult<SysOperationLogVO> operationLogs(int pageNum, int pageSize) {
        IPage<SysOperationLog> page = operationLogs.selectPage(Page.of(pageNum, pageSize), Wrappers.<SysOperationLog>lambdaQuery().orderByDesc(SysOperationLog::getCreateTime));
        return new PageResult<>(EntityVOConverter.toSysOperationLogVOList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 管理员驳回工单（例如信息不全或恶意报修）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long id, AdminOrderActionDTO body) {
        repairOrderService.adminTransition(id, required(body.getVersion(), "version"), 8, required(body.getReason(), "reason"));
    }

    /**
     * 管理员强制关闭工单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Long id, AdminOrderActionDTO body) {
        repairOrderService.adminTransition(id, required(body.getVersion(), "version"), 9, required(body.getReason(), "reason"));
    }

    /**
     * 管理员仲裁有争议的工单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arbitrateOrder(Long id, AdminArbitrateDTO body) {
        repairOrderService.adminTransition(id, required(body.getVersion(), "version")
                , required(body.getTargetStatus(), "targetStatus"), required(body.getReason(), "reason"));
    }

    private void applyDictData(SysDictData data, SysDictDataDTO body) {
        data.setDictType(required(body.getDictType(), "dictType"));
        data.setDictLabel(required(body.getDictLabel(), "dictLabel"));
        data.setDictValue(required(body.getDictValue(), "dictValue"));
        data.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        data.setStatus(defaultZero(body.getStatus()));
        data.setRemark(body.getRemark());
    }

    /**
     * 写入后台操作日志。
     * 所有管理端敏感操作都在同一个业务事务内写日志，业务失败时日志一起回滚，避免伪审计。
     */
    private void audit(String type, String target, Long id, String description) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(SecurityUtils.current().userId());
        log.setOperationType(type);
        log.setTargetType(target);
        log.setTargetId(id);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    private SysUser requireUser(Long id) {
        SysUser user = users.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private SysRole requireRole(String roleCode) {
        SysRole role = roles.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleName, roleCode));
        if (role == null) {
            throw BusinessException.badRequest("角色不存在");
        }
        return role;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Long required(Long value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Integer required(Integer value, String field) {
        if (value == null) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }

    private Integer defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String random() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}