package org.red5.server.net;

import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IServiceCall;

/**
 * Represents a "command" sent to or received from an end-point.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface ICommand {

    int getTransactionId();

    IServiceCall getCall();

    Map<String, Object> getConnectionParams();

    IoBuffer getData();

}
