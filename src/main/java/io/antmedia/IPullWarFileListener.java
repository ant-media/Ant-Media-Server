package io.antmedia;

import java.io.IOException;

public interface IPullWarFileListener {
    public boolean pullWarFile(String appName, String warFileUrl) throws IOException;
}
