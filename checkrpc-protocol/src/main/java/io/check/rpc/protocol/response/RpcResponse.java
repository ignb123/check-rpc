package io.check.rpc.protocol.response;

import io.check.rpc.protocol.base.RpcMessage;

public class RpcResponse extends RpcMessage {

    private static final long serialVersionUID = 425335064405584525L;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 结果数据
     */
    private Object result;

    public boolean isError() {
        return error != null;
    }


    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
