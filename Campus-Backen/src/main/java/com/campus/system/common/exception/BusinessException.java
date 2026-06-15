package com.campus.system.common.exception;

import lombok.Getter;

// 自定义异常
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
      super(message);
      this.code = code;
    }

    public static BusinessException badRequest(String message) {
      return new BusinessException(400, message);
    }

    public static BusinessException unauthorized(String message) {
      return new BusinessException(401, message);
    }

    public static BusinessException forbidden(String message) {
      return new BusinessException(403, message);
    }

    public static BusinessException notFound(String message) {
      return new BusinessException(404, message);
    }

    public static BusinessException conflict(String message) {
      return new BusinessException(409, message);
    }

    public static BusinessException tooMany(String message) {
      return new BusinessException(429, message);
    }
}
