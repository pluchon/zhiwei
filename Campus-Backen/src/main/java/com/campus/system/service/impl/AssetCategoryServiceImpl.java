package com.campus.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.system.common.exception.BusinessException;
import com.campus.system.common.result.PageResult;
import com.campus.system.common.security.SecurityUtils;
import com.campus.system.common.util.NameSortUtil;
import com.campus.system.converter.AssetConverter;
import com.campus.system.dto.AssetCategoryDTO;
import com.campus.system.dto.AssetCategoryQueryDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.system.entity.Asset;
import com.campus.system.entity.AssetCategory;
import com.campus.system.entity.SysOperationLog;
import com.campus.system.mapper.AssetCategoryMapper;
import com.campus.system.mapper.AssetMapper;
import com.campus.system.mapper.SysOperationLogMapper;
import com.campus.system.service.interfaces.AssetCategoryService;
import com.campus.system.vo.AssetCategoryVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 资产分类业务实现
@Service
public class AssetCategoryServiceImpl implements AssetCategoryService {

    @Autowired
    private AssetCategoryMapper categoryMapper;

    @Autowired
    private AssetMapper assetMapper;

    @Autowired
    private SysOperationLogMapper operationLogs;

    @Override
    public List<AssetCategoryVO> enabledList() {
        return AssetConverter.toCategoryVOList(categoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getDeleteState, 0).eq(AssetCategory::getStatus, 0).orderByAsc(AssetCategory::getCategoryName)));
    }

    @Override
    public PageResult<AssetCategoryVO> adminList(int pageNum, int pageSize, AssetCategoryQueryDTO query) {
        AssetCategoryQueryDTO safeQuery = query == null ? new AssetCategoryQueryDTO() : query;
        IPage<AssetCategory> page = categoryMapper.selectPage(Page.of(pageNum, pageSize), buildAdminQueryWrapper(safeQuery));
        return new PageResult<>(AssetConverter.toCategoryVOList(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<AssetCategoryVO> listForExportByIds(List<Long> assetCategoryIds) {
        if (assetCategoryIds == null || assetCategoryIds.isEmpty()) {
            throw BusinessException.badRequest("请先选择要导出的分类");
        }
        if (assetCategoryIds.size() > 1000) {
            throw BusinessException.badRequest("单次最多导出 1000 条");
        }
        List<AssetCategory> list = categoryMapper.selectList(Wrappers.<AssetCategory>lambdaQuery().eq(AssetCategory::getDeleteState, 0).in(AssetCategory::getAssetCategoryId, assetCategoryIds));
        if (list.size() != assetCategoryIds.size()) {
            throw BusinessException.badRequest("部分资产分类不存在");
        }
        Map<Long, AssetCategory> categoryMap = list.stream()
                .collect(Collectors.toMap(AssetCategory::getAssetCategoryId, item -> item, (left, right) -> left));
        List<AssetCategory> ordered = assetCategoryIds.stream().map(categoryMap::get).filter(Objects::nonNull).toList();
        return AssetConverter.toCategoryVOList(ordered);
    }

    @Override
    public List<AssetCategoryVO> listForExport(AssetCategoryQueryDTO query) {
        AssetCategoryQueryDTO safeQuery = query == null ? new AssetCategoryQueryDTO() : query;
        IPage<AssetCategory> page = categoryMapper.selectPage(Page.of(1, 1001), buildAdminQueryWrapper(safeQuery));
        if (page.getTotal() > 1000) {
            throw BusinessException.badRequest("导出数量超过 1000 条，请缩小筛选范围");
        }
        return AssetConverter.toCategoryVOList(page.getRecords());
    }

    private LambdaQueryWrapper<AssetCategory> buildAdminQueryWrapper(AssetCategoryQueryDTO query) {
        LambdaQueryWrapper<AssetCategory> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AssetCategory::getDeleteState, 0);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(AssetCategory::getCategoryName, query.getKeyword().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AssetCategory::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(AssetCategory::getCategoryName);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetCategoryVO create(AssetCategoryDTO body) {
        AssetCategory category = new AssetCategory();
        applyName(category, required(body.getCategoryName(), "categoryName"));
        category.setStatus(0);
        categoryMapper.insert(category);
        audit("CREATE_ASSET_CATEGORY", category.getAssetCategoryId(), "新增资产分类 " + category.getCategoryName());
        return AssetConverter.toCategoryVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssetCategoryVO update(Long id, AssetCategoryDTO body) {
        AssetCategory category = requireCategory(id);
        if (body.getCategoryName() != null) {
            applyName(category, required(body.getCategoryName(), "categoryName"));
        }
        if (body.getStatus() != null) {
            category.setStatus(body.getStatus());
        }
        categoryMapper.updateById(category);
        audit("UPDATE_ASSET_CATEGORY", id, "修改资产分类 " + category.getCategoryName());
        return AssetConverter.toCategoryVO(categoryMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        AssetCategory category = requireCategory(id);
        long assetCount = assetMapper.selectCount(Wrappers.<Asset>lambdaQuery().eq(Asset::getAssetCategoryId, id).eq(Asset::getDeleteState, 0));
        if (assetCount > 0) {
            throw BusinessException.conflict("分类下仍存在未删除资产，不能停用");
        }
        category.setStatus(1);
        categoryMapper.updateById(category);
        audit("DISABLE_ASSET_CATEGORY", id, "停用资产分类 " + category.getCategoryName());
    }

    private void applyName(AssetCategory category, String name) {
        String trimmed = NameSortUtil.trimName(name);
        String normalized = NameSortUtil.normalizeName(trimmed);
        AssetCategory existing = categoryMapper.selectOne(Wrappers.<AssetCategory>lambdaQuery()
                .eq(AssetCategory::getNormalizedName, normalized).eq(AssetCategory::getDeleteState, 0));
        if (existing != null && !existing.getAssetCategoryId().equals(category.getAssetCategoryId())) {
            throw BusinessException.conflict("分类名称已存在");
        }
        category.setCategoryName(trimmed);
        category.setNormalizedName(normalized);
    }

    private AssetCategory requireCategory(Long id) {
        AssetCategory category = categoryMapper.selectById(id);
        if (category == null || category.getDeleteState() != 0) {
            throw BusinessException.notFound("资产分类不存在");
        }
        return category;
    }

    private void audit(String type, Long targetId, String description) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(SecurityUtils.current().userId());
        log.setOperationType(type);
        log.setTargetType("ASSET_CATEGORY");
        log.setTargetId(targetId);
        log.setDescription(description);
        operationLogs.insert(log);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest(field + " 不能为空");
        }
        return value;
    }
}
