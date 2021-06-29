package org.red5.server.net.rtmp.event;

import java.util.Arrays;

import org.red5.server.api.service.IPendingServiceCallback;

/**
 * Represents an invoke to be executed on a connected client.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ClientInvokeEvent extends BaseEvent {

    private final String method;

    private final Object[] params;

    private final IPendingServiceCallback callback;

    public ClientInvokeEvent(String method, Object[] params, IPendingServiceCallback callback) {
        super(Type.CLIENT_INVOKE);
        this.method = method;
        this.params = params;
        this.callback = callback;
    }

    public final static ClientInvokeEvent build(String method, Object[] params, IPendingServiceCallback callback) {
        ClientInvokeEvent event = new ClientInvokeEvent(method, params, callback);
        return event;
    }

    @Override
    public byte getDataType() {
        return TYPE_INVOKE;
    }

    @Override
    protected void releaseInternal() {
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return the params
     */
    public Object[] getParams() {
        return params;
    }

    /**
     * @return the callback
     */
    public IPendingServiceCallback getCallback() {
        return callback;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ClientInvokeEvent [method=" + method + ", params=" + Arrays.toString(params) + ", callback=" + callback + "]";
    }

}
