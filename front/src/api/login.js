import request from "@/utils/request";

// 密码登录
export function loginByPassword(data) {
  return request({
    url: "/auth/login/password",
    headers: {
      isToken: false,
      repeatSubmit: false,
    },
    method: "post",
    data,
  });
}

// 手机快捷登录
export function loginByPhone(data) {
  return request({
    url: "/auth/login/phone",
    headers: {
      isToken: false,
      repeatSubmit: false,
    },
    method: "post",
    data,
  });
}

// 获取当前登录用户的信息
export function getInfo() {
  return request({
    url: "/users/me",
    method: "get",
  });
}

// 退出登录
export function logout() {
  return request({
    url: "/auth/logout",
    method: "post",
  });
}
