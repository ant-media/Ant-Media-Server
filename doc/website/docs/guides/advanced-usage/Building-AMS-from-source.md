# Building AMS from source

#### Linux (Ubuntu)

A couple of commons repos should be cloned and build with maven.

*   Go to a directory where you will clone repos
*   Clone and build ant-media-server-parent
    
        $ git clone https://github.com/ant-media/ant-media-server-parent.git
        $ cd ant-media-server-parent/
        $ mvn clean install -Dgpg.skip=true
        $ cd ..
    

#### Building Community Edition

*   Build the Web Panel
    *   Install Node
        
            $ wget https://nodejs.org/download/release/v14.8.0/node-v14.8.0-linux-x64.tar.gz
            $ tar -zxf node-v14.8.0-linux-x64.tar.gz
            $ export PATH=$PATH:`pwd`/node-v14.8.0-linux-x64/bin
            $ npm install -g @angular/cli@10.0.5 
        
    *   Build
        
            $ git clone --depth=1 https://github.com/ant-media/ManagementConsole_AngularApp.git;
            $ cd ManagementConsole_AngularApp
            $ npm install
            $ ng build --prod
            $ cp -a ./dist/. ../Ant-Media-Server/src/main/server/webapps/root/
        
*   Clone, build and package Ant-Media-Server
    
        $ git clone https://github.com/ant-media/Ant-Media-Server.git
        $ cd Ant-Media-Server
        $ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true
        $ ./repackage_community.sh
    

### Building Enterprise Edition

*   Clone, build Ant-Media-Server
    
        $ git clone https://github.com/ant-media/Ant-Media-Server.git
        $ cd Ant-Media-Server
        $ mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true
    
*   Build Ant-Media-Enterprise Source code of Ant-Media-Enterprise is provided to the Enterprise users
    
        $ cd /where/you/download/enterprise/repo
        $ ./redeploy.sh
    
*   Build the Filter Plugin
    
        $ git clone https://github.com/ant-media/Plugins.git
        $ cd Plugins/FilterPlugin
        $ mvn install -Dmaven.test.skip=true -Dgpg.skip=true
    
*   Build the Web Panel
    *   Install Node
        
            $ wget https://nodejs.org/download/release/v14.8.0/node-v14.8.0-linux-x64.tar.gz
            $ tar -zxf node-v14.8.0-linux-x64.tar.gz
            $ export PATH=$PATH:`pwd`/node-v14.8.0-linux-x64/bin
            $ npm install -g @angular/cli@10.0.5 
        
    *   Build
        
            $ git clone --depth=1 https://github.com/ant-media/ManagementConsole_AngularApp.git;
            $ cd ManagementConsole_AngularApp
            $ npm install
            $ ng build --prod
            $ cp -a ./dist/. ../Ant-Media-Server/src/main/server/webapps/root/
        
    *   Package Enterprise Edition
        
            $ cd Ant-Media-Server
            $ ./repackage_enterprise.sh
        

If everything goes well, a new packaged Ant Media Server(ant-media-server-x.x.x.zip) file will be created in Ant-Media-Server/target directory