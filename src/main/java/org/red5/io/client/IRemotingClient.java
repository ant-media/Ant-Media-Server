package org.red5.io.client;

public interface IRemotingClient {

    Object invokeMethod(String method, Object[] params);

}
