import {
  adminList,
  adminCreate,
  adminUpdate,
  adminDelete,
  exportUsers,
} from "@/api/admin";
import { ElMessage, ElMessageBox } from "element-plus";
import { parseTime, blobValidate } from "@/utils/ruoyi";
import { saveAs } from "file-saver";

// 接收组件属性
const props = defineProps({
  resource: String,
  title: String,
  columns: Array,
  fields: { type: Array, default: () => [] },
  creatable: Boolean,
  description: String,
});

// 系统预置的角色下拉选项
const roleOptions = [
  { label: "学生", value: "STUDENT", roleId: 1 },
  { label: "教师", value: "TEACHER", roleId: 2 },
  { label: "维修师傅", value: "REPAIRER", roleId: 3 },
  { label: "管理员", value: "ADMIN", roleId: 4, disabled: true },
];

// 系统启停状态下拉选项
const statusOptions = [
  { label: "正常 / 启用", value: 0 },
  { label: "停用", value: 1 },
];

// 激活状态下拉选项
const activationOptions = [
  { label: "未激活", value: 0 },
  { label: "已激活", value: 1 },
];

// 页面基础交互状态
const loading = ref(false);
const saving = ref(false);
const exporting = ref(false);
const exportMode = ref(false);
const tableRef = ref(null);
const selectedRows = ref([]);

// 对话框显示与模式控制（模式：create/edit）
const dialog = ref(false);
const dialogMode = ref("create");

// 数据列表及分页状态
const rows = ref([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10 });

const userQuery = reactive({
  keyword: "",
  roleIds: [],
  activationStatuses: [],
  accountStatuses: [],
});

const roleFilterOptions = [
  { label: "学生", value: 1 },
  { label: "教师", value: 2 },
  { label: "维修师傅", value: 3 },
  { label: "管理员", value: 4 },
];

const activationFilterOptions = [
  { label: "未激活", value: 0 },
  { label: "已激活", value: 1 },
];

const accountFilterOptions = [
  { label: "正常 / 启用", value: 0 },
  { label: "停用", value: 1 },
];

// 维修能力弹窗内的搜索关键词
const searchKeywords = reactive({
  repairer: "",
  category: "",
});

// 表单实体数据
const form = reactive({});

// 辅助下拉数据字典
const helper = reactive({
  users: [],
  repairers: [],
  categories: [],
  dictTypes: [],
});

// 计算当前资源是否需要后端支持物理分页
const isPaged = computed(() => ["users", "audit/login-logs", "audit/operation-logs"].includes(props.resource));

const isUserResource = computed(() => props.resource === "users");

const isCategoriesResource = computed(() => props.resource === "categories");

const isCapabilitiesResource = computed(() => props.resource === "capabilities");

const isDictResource = computed(() => props.resource === "dicts/data");

const isAuditLogResource = computed(() =>
  ["audit/login-logs", "audit/operation-logs"].includes(props.resource),
);

const isCompactResource = computed(
  () =>
    isCategoriesResource.value ||
    isCapabilitiesResource.value ||
    isDictResource.value ||
    isAuditLogResource.value,
);

const showTableFooterCreate = computed(
  () =>
    props.creatable &&
    (isCategoriesResource.value || isCapabilitiesResource.value || isDictResource.value),
);

const hideDialogHint = computed(
  () => isCategoriesResource.value || isCapabilitiesResource.value || isDictResource.value,
);

const hidePageHead = computed(() => isCompactResource.value);

const selectedCount = computed(() => selectedRows.value.length);

// 计算当前资源是否支持在行内进行编辑修改
const canEdit = computed(() => ["users", "categories", "dicts/data"].includes(props.resource));

// 计算当前资源是否支持在行内直接物理删除
const canRemove = computed(() => props.resource === "capabilities");

// 根据不同资源类型返回对应的业务使用提示语
const resourceHint = computed(() => {
  if (props.description) {
    return props.description;
  }
  if (props.resource === "users") {
    return "创建学生、教师或维修师傅账号；管理员账号不允许在后台新增或提升。";
  }
  if (props.resource === "capabilities") {
    return "维修能力会影响维修师傅可见的待接工单，请选择维修师傅和可处理的故障类型。";
  }
  if (props.resource === "dicts/data") {
    return "原因字典用于退回、驳回和关闭工单时的原因选项，建议保留 OTHER 兜底项。";
  }
  if (props.resource === "categories") {
    return "故障类型用于报修分类和维修能力匹配，停用后不建议继续创建新报修。";
  }
  return "这里展示后台审计和基础管理数据。";
});

// 计算弹窗表单的标题
const dialogTitle = computed(() => {
  if (props.resource === "users") {
    return dialogMode.value === "create" ? "新增用户" : "编辑用户";
  }
  return dialogMode.value === "create" ? `新增${props.title}` : `编辑${props.title}`;
});

// 根据资源和弹窗模式计算动态表单字段列表
const formFields = computed(() => {
  if (props.resource === "users") {
    return dialogMode.value === "create" ? userCreateFields() : userEditFields();
  }
  if (props.resource === "categories") {
    return categoryFields();
  }
  if (props.resource === "capabilities") {
    return capabilityFields();
  }
  if (props.resource === "dicts/data") {
    return dictFields();
  }
  return props.fields;
});

function buildUserParams({ includePagination = true } = {}) {
  const params = {};
  if (includePagination) {
    params.pageNum = query.pageNum;
    params.pageSize = query.pageSize;
  }
  const keyword = userQuery.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  if (userQuery.roleIds.length) {
    params.roleIds = userQuery.roleIds.map(String).join(",");
  }
  if (userQuery.activationStatuses.length) {
    params.activationStatuses = userQuery.activationStatuses.map(String).join(",");
  }
  if (userQuery.accountStatuses.length) {
    params.accountStatuses = userQuery.accountStatuses.map(String).join(",");
  }
  return params;
}

function searchUsers() {
  query.pageNum = 1;
  if (exportMode.value) {
    clearSelection();
  }
  load();
}

function resetUserFilters() {
  userQuery.keyword = "";
  userQuery.roleIds = [];
  userQuery.activationStatuses = [];
  userQuery.accountStatuses = [];
  searchUsers();
}

function onSelectionChange(rows) {
  selectedRows.value = rows;
}

function enterExportMode() {
  exportMode.value = true;
  selectedRows.value = [];
  nextTick(() => {
    tableRef.value?.clearSelection();
  });
}

function exitExportMode() {
  exportMode.value = false;
  selectedRows.value = [];
  nextTick(() => {
    tableRef.value?.clearSelection();
  });
}

function selectCurrentPage() {
  rows.value.forEach((row) => {
    tableRef.value?.toggleRowSelection(row, true, true);
  });
}

function clearSelection() {
  selectedRows.value = [];
  tableRef.value?.clearSelection();
}

async function doExport(params) {
  exporting.value = true;
  try {
    const blob = await exportUsers(params);
    if (!blobValidate(blob)) {
      const text = await blob.text();
      const result = JSON.parse(text);
      ElMessage.error(result.message || "导出失败，请缩小筛选范围");
      return;
    }
    saveAs(blob, `users_${parseTime(new Date(), "{y}{m}{d}{h}{i}{s}")}.xlsx`);
    ElMessage.success("导出成功");
    exitExportMode();
    await load();
  } catch (error) {
    ElMessage.error(error?.message || "导出失败");
  } finally {
    exporting.value = false;
  }
}

async function exportFiltered() {
  if (!total.value) {
    ElMessage.warning("当前没有可导出的用户");
    return;
  }
  await doExport(buildUserParams({ includePagination: false }));
}

async function exportSelected() {
  const rows = tableRef.value?.getSelectionRows() ?? [];
  const userIds = [
    ...new Set(
      rows
        .map((row) => row.userId)
        .filter((id) => id !== null && id !== undefined && id !== ""),
    ),
  ];
  if (!userIds.length) {
    ElMessage.warning("请先勾选要导出的用户");
    return;
  }
  await doExport({ userIds: userIds.map(String).join(",") });
}

// 异步加载列表数据及关联辅助字典选项
async function load() {
  loading.value = true;
  try {
    await loadHelperOptions();
    const params = isUserResource.value ? buildUserParams() : query;
    const result = await adminList(props.resource, params);
    rows.value = normalizeRecords(result);
    total.value = Number(result?.total || 0);
  } finally {
    loading.value = false;
  }
}

// 依据当前管理的资源按需异步拉取对应的下拉关联数据
async function loadHelperOptions() {
  if (props.resource === "capabilities") {
    await loadUsersAndCategories();
    return;
  }
  if (props.resource === "dicts/data") {
    await loadDictTypes();
    return;
  }
  if (props.resource === "users") {
    await loadUsers();
  }
}

// 批量加载用户与故障分类
async function loadUsersAndCategories() {
  await Promise.all([loadUsers(), loadCategories()]);
}

// 异步加载所有用户列表并提取师傅列表
async function loadUsers() {
  const result = await adminList("users", { pageNum: 1, pageSize: 200 });
  helper.users = normalizeRecords(result);
  helper.repairers = helper.users.filter((user) => Number(user.roleId) === 3);
}

// 异步加载所有故障分类列表
async function loadCategories() {
  const result = await adminList("categories");
  helper.categories = normalizeRecords(result);
}

// 异步加载所有数据字典类型列表
async function loadDictTypes() {
  const result = await adminList("dicts/types");
  helper.dictTypes = normalizeRecords(result);
}

// 规范化后端返回的分页数据或普通数组格式
function normalizeRecords(result) {
  if (Array.isArray(result)) {
    return result;
  }
  return result?.records || [];
}

// 触发打开新增数据对话框
function openCreate() {
  dialogMode.value = "create";
  resetForm();
  applyDefaults();
  dialog.value = true;
}

// 触发打开编辑数据对话框并执行表单数据回显
function openEdit(row) {
  dialogMode.value = "edit";
  resetForm();
  Object.assign(form, pickEditable(row));
  dialog.value = true;
}

// 重置对话框表单属性
function resetForm() {
  Object.keys(form).forEach((key) => delete form[key]);
  searchKeywords.repairer = "";
  searchKeywords.category = "";
}

// 写入表单项的新增默认预设值
function applyDefaults() {
  form.status = 0;
  form.accountStatus = 0;
  form.sortOrder = 0;
  if (props.resource === "users") {
    form.roleCode = "STUDENT";
    form.initialPassword = "Campus123!";
  }
}

// 提取当前资源类型下可被编辑修改的特定字段属性，防止冗余属性污染提交
function pickEditable(row) {
  if (props.resource === "users") {
    return {
      userId: row.userId,
      roleCode: roleCodeById(row.roleId),
      accountStatus: row.accountStatus,
      nickName: row.nickName,
      phoneNumber: row.phoneNumber,
    };
  }
  if (props.resource === "categories") {
    return {
      categoryId: row.categoryId,
      categoryName: row.categoryName,
      description: row.description,
      status: row.status,
    };
  }
  if (props.resource === "dicts/data") {
    return {
      dictDataId: row.dictDataId,
      dictType: row.dictType,
      dictLabel: row.dictLabel,
      dictValue: row.dictValue,
      status: row.status,
      remark: row.remark,
    };
  }
  return { ...row };
}

// 提交数据（包含新增和保存修改两个入口）
async function submit() {
  saving.value = true;
  try {
    if (dialogMode.value === "create") {
      await adminCreate(props.resource, buildPayload());
      ElMessage.success("新增成功");
    } else {
      await adminUpdate(props.resource, rowId(form), buildPayload());
      ElMessage.success("保存成功");
    }
    dialog.value = false;
    resetForm();
    await load();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "操作失败");
  } finally {
    saving.value = false;
  }
}

// 构建接口请求的载荷，过滤掉只读字段与空值
function buildPayload() {
  const payload = {};
  formFields.value.forEach((field) => {
    if (field.readonly) {
      return;
    }
    if (form[field.key] !== undefined && form[field.key] !== "") {
      payload[field.key] = form[field.key];
    }
  });
  if (props.resource === "dicts/data" && dialogMode.value === "create") {
    payload.sortOrder = 0;
  }
  return payload;
}

// 获取各行资源的主键ID值
function rowId(row) {
  return row.userId || row.categoryId || row.dictDataId || row.capabilityId;
}

// 执行数据物理删除
async function remove(row) {
  await ElMessageBox.confirm("确定移除这条维修能力吗？", "操作确认", {
    type: "warning",
    confirmButtonText: "移除",
    cancelButtonText: "取消",
  });
  await adminDelete(props.resource, rowId(row));
  ElMessage.success("已移除");
  await load();
}

// 在表格列中处理与映射展示文本
function displayValue(row, column) {
  const value = row[column.prop];
  if (column.prop === "roleId") {
    return roleLabelById(value);
  }
  if (column.prop === "activationStatus") {
    return optionLabel(activationOptions, value);
  }
  if (column.prop === "accountStatus" || column.prop === "status") {
    return optionLabel(statusOptions, value);
  }
  if (column.prop === "repairerId") {
    return userLabel(value);
  }
  if (column.prop === "categoryId") {
    return categoryLabel(value);
  }
  if (column.prop === "dictType") {
    return dictTypeLabel(value);
  }
  return value ?? "-";
}

// 根据状态列的值计算 Tag 组件的配色类型
function tagType(row, prop) {
  const value = row[prop];
  if (prop === "activationStatus") {
    return Number(value) === 1 ? "success" : "warning";
  }
  if (prop === "accountStatus" || prop === "status") {
    return Number(value) === 0 ? "success" : "danger";
  }
  return "";
}

// 判断当前列是否需要包装成 Tag 展示
function isTagColumn(prop) {
  return ["activationStatus", "accountStatus", "status"].includes(prop);
}

// 定义创建用户表单的动态字段结构
function userCreateFields() {
  return [
    { key: "userNo", label: "登录账号", placeholder: "请输入学号、工号或自定义账号" },
    { key: "realName", label: "真实姓名", placeholder: "请输入真实姓名" },
    { key: "nickName", label: "展示昵称", placeholder: "选填，不填则与姓名相同" },
    {
      key: "roleCode",
      label: "角色",
      type: "select",
      placeholder: "请选择用户角色",
      options: roleOptions.filter((role) => !role.disabled),
    },
    { key: "phoneNumber", label: "手机号", placeholder: "请输入11位手机号" },
    { key: "parentPhone", label: "家长手机号", placeholder: "学生可填，其他角色可不填" },
    { key: "initialPassword", label: "初始密码", type: "password", placeholder: "默认 Campus123!，可按需修改" },
  ];
}

// 定义编辑用户表单的动态字段结构
function userEditFields() {
  return [
    {
      key: "roleCode",
      label: "角色",
      type: "select",
      placeholder: "请选择用户角色",
      options: roleOptions.filter((role) => !role.disabled),
    },
    {
      key: "accountStatus",
      label: "账号状态",
      type: "select",
      placeholder: "请选择账号状态",
      options: statusOptions,
    },
    { key: "nickName", label: "展示昵称", placeholder: "选填，用于页面展示" },
    { key: "phoneNumber", label: "手机号", placeholder: "请输入11位手机号" },
  ];
}

// 定义分类表单的动态字段结构
function categoryFields() {
  return [
    { key: "categoryName", label: "类型名称", placeholder: "例如：水电维修", half: true },
    { key: "status", label: "状态", type: "select", options: statusOptions, half: true },
    { key: "description", label: "类型说明", type: "textarea" },
  ];
}

function capabilityFields() {
  return [
    { key: "repairerId", label: "维修师傅", type: "searchSelect", optionType: "repairer" },
    { key: "categoryId", label: "故障类型", type: "searchSelect", optionType: "category" },
  ];
}

// 定义字典数据表单的动态字段结构
function dictFields() {
  return [
    {
      key: "dictType",
      label: "字典类型",
      type: "select",
      options: dictTypeOptions(),
      half: true,
      placeholder: "选择所属字典",
    },
    {
      key: "status",
      label: "状态",
      type: "select",
      options: statusOptions,
      half: true,
    },
    {
      key: "dictLabel",
      label: "显示文本",
      half: true,
      placeholder: "用户选择时看到的文案",
    },
    {
      key: "dictValue",
      label: "业务值",
      half: true,
      placeholder: "系统内部标识，如 OTHER",
    },
    {
      key: "remark",
      label: "备注",
      type: "textarea",
      placeholder: "选填，补充说明",
    },
  ];
}

// 映射师傅选项下拉列表
function repairerOptions() {
  return helper.repairers.map((user) => ({
    label: `${user.realName || user.nickName}（${user.userNo}）`,
    value: user.userId,
  }));
}

// 映射分类选项下拉列表
function categoryOptions() {
  return helper.categories.map((category) => ({
    label: category.categoryName,
    value: category.categoryId,
  }));
}

function filteredSearchSelectOptions(optionType) {
  const keyword = (searchKeywords[optionType] || "").trim().toLowerCase();
  const options = optionType === "repairer" ? repairerOptions() : categoryOptions();
  if (!keyword) {
    return options;
  }
  return options.filter((option) => option.label.toLowerCase().includes(keyword));
}

function searchSelectPlaceholder(optionType) {
  return optionType === "repairer" ? "搜索师傅姓名或工号" : "搜索故障类型名称";
}

// 将字典类型列表转化为下拉选项
function dictTypeOptions() {
  return helper.dictTypes.map((item) => ({
    label: `${item.dictName}（${item.dictType}）`,
    value: item.dictType,
  }));
}

// 依据角色ID返回名称
function roleLabelById(roleId) {
  return roleOptions.find((item) => Number(item.roleId) === Number(roleId))?.label || roleId;
}

// 依据角色ID返回代码
function roleCodeById(roleId) {
  return roleOptions.find((item) => Number(item.roleId) === Number(roleId))?.value;
}

// 依据用户ID显示真实姓名和登录名
function userLabel(userId) {
  const user = helper.users.find((item) => Number(item.userId) === Number(userId));
  return user ? `${user.realName || user.nickName}（${user.userNo}）` : userId;
}

// 依据分类ID显示分类名
function categoryLabel(categoryId) {
  return helper.categories.find((item) => Number(item.categoryId) === Number(categoryId))?.categoryName || categoryId;
}

// 依据字典类型标识返回中文描述
function dictTypeLabel(dictType) {
  const item = helper.dictTypes.find((candidate) => candidate.dictType === dictType);
  return item ? `${item.dictName}（${dictType}）` : dictType;
}

// 通用选项中匹配显示标签
function optionLabel(options, value) {
  return options.find((item) => Number(item.value) === Number(value))?.label || value;
}

// 监听资源类型改变，刷新数据和重置状态
watch(
  () => props.resource,
  async () => {
    query.pageNum = 1;
    resetForm();
    if (exportMode.value) {
      exitExportMode();
    }
    if (isUserResource.value) {
      userQuery.keyword = "";
      userQuery.roleIds = [];
      userQuery.activationStatuses = [];
      userQuery.accountStatuses = [];
    }
    await load();
  },
);

// 挂载组件时初始化加载
onMounted(load);
