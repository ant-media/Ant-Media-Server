/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.stream;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.util.ScopeUtils;

/**
 * Default filename generator for streams. The files will be stored in a directory "streams" in the application folder. Option for changing directory streams are saved to is investigated as of 0.6RC1.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (bauch@struktur.de)
 */
public class DefaultStreamFilenameGenerator implements IStreamFilenameGenerator {

    /**
     * Generate stream directory based on relative scope path. The base directory is
     *
     * <pre>
     * streams
     * </pre>
     *
     * , e.g. a scope
     *
     * <pre>
     * /application/one/two/
     * </pre>
     *
     * will generate a directory
     *
     * <pre>
     * /streams/one/two/
     * </pre>
     *
     * inside the application.
     *
     * @param scope
     *            Scope
     * @return Directory based on relative scope path
     */
    private String getStreamDirectory(IScope scope) {
        final StringBuilder result = new StringBuilder();
        final IScope app = ScopeUtils.findApplication(scope);
        final String prefix = "streams/";
        while (scope != null && scope != app) {
            result.insert(0, scope.getName() + "/");
            scope = scope.getParent();
        }
        if (result.length() == 0) {
            return prefix;
        } else {
            result.insert(0, prefix).append('/');
            return result.toString();
        }
    }

    /** {@inheritDoc} */
    public String generateFilename(IScope scope, String name, GenerationType type) {
        return generateFilename(scope, name, null, type, null);
    }

    /** {@inheritDoc} */

    public String generateFilename(IScope scope, String name, String extension, GenerationType type, String subFolder) {
        String result;
        if(subFolder != null) {
            subFolder= subFolder.replace("..","");
            result = getStreamDirectory(scope) + "/" + subFolder + "/" + name;
        }
        else
            result = getStreamDirectory(scope) + "/" + name;

        if (extension != null && !extension.equals("")) {
            result += extension;
        }
        return result;
    }

    /**
     * The default filenames are relative to the scope path, so always return
     *
     * <pre>
     * false
     * </pre>
     *
     * .
     *
     * @return always
     *
     *         <pre>
     * false
     * </pre>
     */
    public boolean resolvesToAbsolutePath() {
        return false;
    }

}
