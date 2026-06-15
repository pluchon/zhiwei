import { createRouter, createWebHistory } from "vue-router";
import Layout from "@/layout";

function createFlatLayoutRoute(basePath, roles, childRoute) {
  return {
    path: basePath,
    component: Layout,
    roles,
    meta: childRoute.meta,
    children: [
      {
        path: "",
        ...childRoute,
      },
    ],
  };
}

export const constantRoutes = [
  {
    path: "/redirect",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("@/views/redirect/index.vue"),
      },
    ],
  },
  {
    path: "/login",
    component: () => import("@/views/login.vue"),
    hidden: true,
  },
  {
    path: "/activation",
    component: () => import("@/views/auth/activation.vue"),
    hidden: true,
    meta: { title: "账号激活" },
  },
  {
    path: "/recovery",
    component: () => import("@/views/auth/recovery.vue"),
    hidden: true,
    meta: { title: "账号恢复" },
  },
  {
    path: "/manual-recovery/:recoveryId",
    component: () => import("@/views/auth/manual-recovery-verify.vue"),
    hidden: true,
    meta: { title: "验证新手机号" },
  },
  {
    path: "/401",
    component: () => import("@/views/error/401.vue"),
    hidden: true,
  },
  {
    path: "",
    component: Layout,
    redirect: "/index",
    children: [
      {
        path: "/index",
        component: () => import("@/views/index.vue"),
        name: "Index",
        meta: { title: "首页", icon: "dashboard", affix: true },
      },
    ],
  },
  {
    path: "/user",
    component: Layout,
    hidden: true,
    children: [
      {
        path: "profile",
        component: () => import("@/views/profile/index.vue"),
        name: "Profile",
        meta: { title: "个人中心", icon: "user" },
      },
    ],
  },
];

export const asyncRoutes = [
  createFlatLayoutRoute("/repair/create", ["STUDENT", "TEACHER"], {
    component: () => import("@/views/repair/create.vue"),
    name: "RepairCreate",
    meta: { title: "提交报修", icon: "form" },
  }),
  createFlatLayoutRoute("/repair/mine", ["STUDENT", "TEACHER"], {
    component: () => import("@/views/repair/list.vue"),
    props: { reporter: true, title: "报修记录" },
    name: "RepairMine",
    meta: { title: "报修记录", icon: "list" },
  }),
  createFlatLayoutRoute("/work-order/available", ["REPAIRER"], {
    component: () => import("@/views/repair/list.vue"),
    props: { available: true, title: "待接工单" },
    name: "AvailableOrders",
    meta: { title: "待接工单", icon: "list" },
  }),
  createFlatLayoutRoute("/work-order/mine", ["REPAIRER"], {
    component: () => import("@/views/repair/list.vue"),
    props: { title: "我的维修工单" },
    name: "AssignedOrders",
    meta: { title: "我的工单", icon: "list" },
  }),
  createFlatLayoutRoute("/work-order/suggestion/mine", ["REPAIRER"], {
    component: () => import("@/views/repair/suggestion/mine.vue"),
    name: "RepairerSuggestionMine",
    meta: { title: "我的建议", icon: "documentation" },
  }),
  {
    path: "/assets",
    component: Layout,
    redirect: "/assets/browse",
    roles: ["STUDENT", "TEACHER", "REPAIRER", "ADMIN"],
    children: [
      {
        path: "browse",
        component: () => import("@/views/asset/list.vue"),
        name: "AssetBrowse",
        meta: { title: "资产浏览", icon: "component" },
      },
    ],
  },
  createFlatLayoutRoute("/admin/orders", ["ADMIN"], {
    component: () => import("@/views/repair/list.vue"),
    props: { title: "全部工单" },
    name: "AdminOrders",
    meta: { title: "工单管理", icon: "list" },
  }),
  createFlatLayoutRoute("/admin/users", ["ADMIN"], {
    component: () => import("@/views/admin/resource.vue"),
    props: {
      resource: "users",
      title: "用户管理",
      creatable: true,
      columns: [
        { prop: "userId", label: "ID", width: 90 },
        { prop: "userNo", label: "账号" },
        { prop: "realName", label: "姓名" },
        { prop: "roleId", label: "角色" },
        { prop: "phoneNumber", label: "手机号" },
        { prop: "activationStatus", label: "激活状态" },
        { prop: "accountStatus", label: "账号状态" },
      ],
      fields: [
        { key: "userNo", label: "账号" },
        { key: "realName", label: "真实姓名" },
        { key: "nickName", label: "昵称" },
        { key: "roleCode", label: "角色编码" },
        { key: "phoneNumber", label: "手机号" },
        { key: "initialPassword", label: "初始密码" },
      ],
    },
    name: "AdminUsers",
    meta: { title: "用户管理", icon: "user" },
  }),
  createFlatLayoutRoute("/admin/locations", ["ADMIN"], {
    component: () => import("@/views/admin/location/index.vue"),
    name: "AdminLocations",
    meta: { title: "位置维护", icon: "tree" },
  }),
  createFlatLayoutRoute("/admin/asset-categories", ["ADMIN"], {
    component: () => import("@/views/admin/asset-category/index.vue"),
    name: "AdminAssetCategories",
    meta: { title: "资产分类", icon: "dict" },
  }),
  createFlatLayoutRoute("/admin/assets", ["ADMIN"], {
    component: () => import("@/views/admin/asset/index.vue"),
    name: "AdminAssets",
    meta: { title: "资产台账", icon: "component" },
  }),
  {
    path: "/admin/asset-import",
    component: Layout,
    roles: ["ADMIN"],
    meta: { title: "资产导入", icon: "upload" },
    children: [
      {
        path: "",
        component: () => import("@/views/admin/asset-import/index.vue"),
        name: "AdminAssetImport",
        meta: { title: "资产导入", icon: "upload" },
      },
      {
        path: ":batchId",
        component: () => import("@/views/admin/asset-import/detail.vue"),
        name: "AdminAssetImportDetail",
        hidden: true,
        meta: { title: "导入批次详情", activeMenu: "/admin/asset-import" },
      },
    ],
  },
  createFlatLayoutRoute("/admin/manual-account-recovery", ["ADMIN"], {
    component: () => import("@/views/admin/manual-account-recovery/index.vue"),
    name: "AdminManualRecovery",
    meta: { title: "账号恢复", icon: "peoples" },
  }),
  createFlatLayoutRoute("/admin/statistics", ["ADMIN"], {
    component: () => import("@/views/admin/statistics/index.vue"),
    name: "AdminStatistics",
    meta: { title: "管理统计", icon: "chart" },
  }),
  createFlatLayoutRoute("/admin/repairer-availability", ["ADMIN"], {
    component: () => import("@/views/admin/repairer-availability/index.vue"),
    name: "AdminRepairerAvailability",
    meta: { title: "师傅接单状态", icon: "skill" },
  }),
  createFlatLayoutRoute("/admin/suggestions", ["ADMIN"], {
    component: () => import("@/views/admin/suggestion/page.vue"),
    name: "AdminSuggestions",
    meta: { title: "师傅建议", icon: "message" },
  }),
  createFlatLayoutRoute("/admin/categories", ["ADMIN"], {
    component: () => import("@/views/admin/resource.vue"),
    props: {
      resource: "categories",
      title: "故障类型",
      description: "维护报修分类，维修能力和待接工单匹配都会依赖这里的分类。",
      creatable: true,
      columns: [
        { prop: "categoryId", label: "ID" },
        { prop: "categoryName", label: "名称" },
        { prop: "description", label: "说明" },
        { prop: "status", label: "状态" },
      ],
      fields: [
        { key: "categoryName", label: "名称" },
        { key: "description", label: "说明" },
        { key: "status", label: "状态" },
      ],
    },
    name: "AdminCategories",
    meta: { title: "故障类型", icon: "dict" },
  }),
  createFlatLayoutRoute("/admin/capabilities", ["ADMIN"], {
    component: () => import("@/views/admin/resource.vue"),
    props: {
      resource: "capabilities",
      title: "维修能力",
      description: "把维修师傅和可处理的故障类型绑定起来，待接工单会按能力动态匹配。",
      creatable: true,
      columns: [
        { prop: "capabilityId", label: "ID" },
        { prop: "repairerId", label: "维修师傅" },
        { prop: "categoryId", label: "故障类型" },
      ],
      fields: [
        { key: "repairerId", label: "维修师傅" },
        { key: "categoryId", label: "故障类型" },
      ],
    },
    name: "AdminCapabilities",
    meta: { title: "维修能力", icon: "skill" },
  }),
  createFlatLayoutRoute("/admin/dict", ["ADMIN"], {
    component: () => import("@/views/admin/resource.vue"),
    props: {
      resource: "dicts/data",
      title: "原因字典",
      description: "维护工单退回、驳回和关闭时使用的原因选项。",
      creatable: true,
      columns: [
        { prop: "dictType", label: "字典类型" },
        { prop: "dictLabel", label: "显示文本" },
        { prop: "dictValue", label: "字典值" },
        { prop: "status", label: "状态" },
      ],
      fields: [
        { key: "dictType", label: "字典类型" },
        { key: "dictLabel", label: "显示文本" },
        { key: "dictValue", label: "字典值" },
        { key: "status", label: "状态" },
        { key: "remark", label: "备注" },
      ],
    },
    name: "AdminDict",
    meta: { title: "字典管理", icon: "dict" },
  }),
  createFlatLayoutRoute("/admin/login-logs", ["ADMIN"], {
    component: () => import("@/views/admin/resource.vue"),
    props: {
      resource: "audit/login-logs",
      title: "登录日志",
      columns: [
        { prop: "loginIdentifier", label: "登录标识" },
        { prop: "loginType", label: "方式" },
        { prop: "status", label: "状态" },
        { prop: "message", label: "说明" },
        { prop: "createTime", label: "时间" },
      ],
    },
    name: "AdminLoginLogs",
    meta: { title: "登录日志", icon: "logininfor" },
  }),
  createFlatLayoutRoute("/admin/operation-logs", ["ADMIN"], {
    component: () => import("@/views/admin/resource.vue"),
    props: {
      resource: "audit/operation-logs",
      title: "操作日志",
      columns: [
        { prop: "operationType", label: "操作类型" },
        { prop: "targetType", label: "目标类型" },
        { prop: "description", label: "说明" },
        { prop: "createTime", label: "时间" },
      ],
    },
    name: "AdminOperationLogs",
    meta: { title: "操作日志", icon: "log" },
  }),
  {
    path: "/admin",
    redirect: "/admin/orders",
    hidden: true,
    roles: ["ADMIN"],
  },
  {
    path: "/system/dict-data",
    component: Layout,
    hidden: true,
    roles: ["ADMIN"],
    children: [
      {
        path: "index/:dictId(\\d+)",
        component: () => import("@/views/system/dict/data.vue"),
        name: "DictData",
        meta: { title: "字典数据", activeMenu: "/admin/dict" },
      },
    ],
  },
  {
    path: "/notifications",
    component: Layout,
    roles: ["STUDENT", "TEACHER", "REPAIRER", "ADMIN"],
    children: [
      {
        path: "",
        component: () => import("@/views/notification/index.vue"),
        name: "Notifications",
        meta: { title: "站内信", icon: "bell" },
      },
    ],
  },
  {
    path: "/repair/detail",
    component: Layout,
    hidden: true,
    roles: ["STUDENT", "TEACHER", "REPAIRER", "ADMIN"],
    children: [
      {
        path: ":id",
        component: () => import("@/views/repair/detail.vue"),
        name: "RepairDetail",
        meta: { title: "工单详情" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 };
  },
});

export default router;
