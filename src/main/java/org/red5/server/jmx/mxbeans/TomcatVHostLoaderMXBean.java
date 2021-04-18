/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.server.jmx.mxbeans;

import javax.management.MXBean;
import javax.servlet.ServletException;

import org.apache.catalina.Host;
import org.apache.catalina.Valve;

/**
 * Simple mbean interface for Tomcat container virtual host loaders.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@MXBean
public interface TomcatVHostLoaderMXBean {

    public boolean startWebApplication(String applicationName) throws ServletException;

    public boolean getAutoDeploy();

    public void setAutoDeploy(boolean autoDeploy);

    public Host getHost();

    public String getDomain();

    public void setDomain(String domain);

    public void addAlias(String alias);

    public void removeAlias(String alias);

    public org.apache.catalina.Context addContext(String path, String docBase) throws ServletException;

    public void removeContext(String path);

    public void addValve(Valve valve);

    public void removeValve(String valveInfo);

    public boolean getLiveDeploy();

    public void setLiveDeploy(boolean liveDeploy);

    public String getName();

    public void setName(String name);

    public boolean getStartChildren();

    public void setStartChildren(boolean startChildren);

    public boolean getUnpackWARs();

    public void setUnpackWARs(boolean unpackWARs);

    public String getWebappRoot();

    public void setWebappRoot(String webappRoot);

}
