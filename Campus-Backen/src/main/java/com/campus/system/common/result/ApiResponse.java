package com.campus.system.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.MDC;

// 公共数据处理返回
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private String traceId;

    // 通过MDC追踪日志，便于排查指定的某一次的请求，本质上是生成的UUID
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data, MDC.get("traceId"));
    }
}