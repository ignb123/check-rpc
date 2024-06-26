package io.check.rpc.common.exception;

/**
 * @author check
 * @version 1.0.0
 * @description RpcException
 */
public class RpcException extends RuntimeException {
    private static final long serialVersionUID = -6783134254669118520L;

    /**
     * Instantiates a new Serializer exception.
     *
     * @param e the e
     */
    public RpcException(final Throwable e) {
        super(e);
    }

    /**
     * Instantiates a new Serializer exception.
     *
     * @param message the message
     */
    public RpcException(final String message) {
        super(message);
    }

    /**
     * Instantiates a new Serializer exception.
     *
     * @param message   the message
     * @param throwable the throwable
     */
    public RpcException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}

