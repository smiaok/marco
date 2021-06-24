package com.fudan.intellij.plugin.marco.service.remote;



import java.io.Serializable;
import java.util.Objects;

/**
 * 通用结果返回对象
 */

public class CommonResult<T> implements Serializable {

    /**
     * 结果码
     */

    private Long status;

    /**
     * 提示信息
     */

    private String message;

    /**
     * 返回数据
     */

    private T data;

    protected CommonResult() {
    }

    private CommonResult(long status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CommonResult<?> that = (CommonResult<?>) o;
        return Objects.equals(status, that.status) &&
                Objects.equals(message, that.message) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message, data);
    }
}

