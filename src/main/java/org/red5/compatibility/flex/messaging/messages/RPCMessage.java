package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataOutput;

public abstract class RPCMessage extends AbstractMessage {
    private static final long serialVersionUID = -1203255926746881424L;

    private String remoteUsername;

    private String remotePassword;

    public String getRemoteUsername() {
        return this.remoteUsername;
    }

    public void setRemoteUsername(String s) {
        this.remoteUsername = s;
    }

    public String getRemotePassword() {
        return this.remotePassword;
    }

    public void setRemotePassword(String s) {
        this.remotePassword = s;
    }

    @Override
    public void writeExternal(IDataOutput output) {
        super.writeExternal(output);
        output.writeObject(remoteUsername);
        output.writeObject(remotePassword);
    }

    @Override
    protected void addParameters(StringBuilder result) {
        super.addParameters(result);
        result.append("remoteUsername=" + remoteUsername);
    }
}
