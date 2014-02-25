red5-server
===========

Red5 server core

The majority of the Red5 project information continues to reside on Google Code. Post any issues or wiki entries there, until we post otherwise. https://code.google.com/p/red5/

The Red5 users list may be found here: https://groups.google.com/forum/#!forum/red5interest

Stack Overflow
--------------
If you want answers from a broader audience, Stack Overflow may be your best bet.
http://stackoverflow.com/tags/red5/info

Build from Source
-----------------

To build the red5 jars, execute the following on the command line:
```
mvn -Dmaven.test.skip=true -Dclassifier=bootstrap install
```
This will create the jars in the "target" directory of the workspace; this will also skip the unit tests.

To build and package the server in zip and gz, execute the following:
```
mvn dependency:copy-dependencies
```
This will download all the dependencies into the "target" directory under "dependency". The next command will package everything up:
```
mvn -Dmaven.test.skip=true -Dmaven.buildNumber.doUpdate=false -Dclassifier=bootstrap package
```
Right now, this will skip the demos directory but I'm working on a fix. The xml nodes to copy the demos are in the
```
trunk/src/main/server/assembly/server.xml
```
and may be uncommented for a package build, if you have the entire svn tree checked out.

Eclipse
----------

To create the eclipse project files, execute this within the plugin and / or example directories:
```
mvn eclipse:eclipse
```

Then you will be able to import the projects into Eclipse.

Building in Eclipse
-------------------

Install the maven plugin and it will build automatically.

[http://screencast.com/t/2sgjMevf9 Screencast]

Donations
-------------
Donate to the cause using Bitcoin: https://coinbase.com/checkouts/2c5f023d24b12245d17f8ff8afe794d3

<i>Donations are used for beer and snacks</i>
