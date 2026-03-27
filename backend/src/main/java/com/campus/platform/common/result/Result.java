package com.campus.platform.common.result;

import com.campus.platform.common.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private Integer code;
    private String  message;
    private T       data;
    private String  requestId;
    private Long    timestamp;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(Instant.now().toEpochMilli());
        return r;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(Instant.now().toEpochMilli());
        return r;
    }

    public static <T> Result<T> fail(BusinessException e) {
        return fail(e.getCode(), e.getMessage());
    }
}
