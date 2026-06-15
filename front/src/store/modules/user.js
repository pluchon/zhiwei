import { loginByPassword, loginByPhone, logout, getInfo } from "@/api/login";
import { getToken, setToken, removeToken } from "@/utils/auth";
import usePermissionStore from "@/store/modules/permission";
import useNotificationStore from "@/store/modules/notification";
import defAva from "@/assets/images/profile.jpg";

// 定义 Pinia 用户状态存储仓储
const useUserStore = defineStore("user", {
  // 定义核心状态：令牌、ID、登录名、昵称、头像、角色列表、按钮权限列表
  state: () => ({
    token: getToken(),
    id: "",
    userNo: "",
    nickName: "",
    avatar: "",
    roles: [],
    permissions: [],
  }),
  actions: {
    // 账号密码登录动作
    async passwordLogin(data) {
      const result = await loginByPassword(data);
      this.saveToken(result.token);
    },
    // 手机短信快捷登录动作
    async phoneLogin(data) {
      const result = await loginByPhone(data);
      this.saveToken(result.token);
    },
    // 保存本地并更新 Token 状态
    saveToken(token) {
      setToken(token);
      this.token = token;
    },
    // 获取当前登录用户的详实信息并填充状态仓储
    async getInfo() {
      const result = await getInfo();
      const user = result.user || result;
      const roleCode = result.roleCode || user.roleCode;

      this.id = user.userId;
      this.userNo = user.userNo;
      this.nickName = user.nickName || user.realName;
      this.avatar = result.avatarUrl || user.avatar || defAva;
      this.roles = result.roles || (roleCode ? [roleCode] : []);
      this.permissions = result.permissions || [];

      return result;
    },
    // 退出登录并清除本地会话与状态
    async logOut() {
      try {
        await logout();
      } finally {
        this.resetState();
      }
    },
    // 重置并清空所有用户相关的状态和本地缓存
    resetState() {
      this.token = "";
      this.id = "";
      this.userNo = "";
      this.nickName = "";
      this.avatar = "";
      this.roles = [];
      this.permissions = [];
      usePermissionStore().resetRoutes();
      useNotificationStore().reset();
      removeToken();
    },
  },
});

export default useUserStore;
