red5-server
===========

Red5 server core

The majority of the Red5 project information continues to reside on Google Code. Post any issues or wiki entries there, until we post otherwise. https://code.google.com/p/red5/

The Red5 users list may be found here: https://groups.google.com/forum/#!forum/red5interest

Latest version is 1.0.2-RELEASE

*Red5 1.0.2 Release (9 April 2014)*
<a href="https://mega.co.nz/#!FFsV0TIC!eEeGePK30nCv0xF5E7w_6S3b_z8Y9pjzMkp2-UgZTYk">Windows Java7</a> | 
<a href="https://mega.co.nz/#!8EUFwAxR!qJjgtFCs5tY86ZDqolL_nL9SsWradm4BQeOugffZqqs">Tarball</a> | 
<a href="https://mega.co.nz/#!QUNEiDoI!RhT8p660eJImIuI3kRhuZHfRWxtnZTSpp0-va2_wyrw">ZIP</a>

<i>Note on Bootstrap</i>

The bootstrap and shutdown classes have been moved to the red5-service project; the dependency has been added to this projects pom.

Stack Overflow
--------------
If you want answers from a broader audience, Stack Overflow may be your best bet.
http://stackoverflow.com/tags/red5/info

Build from Source
-----------------

To build the red5 jars, execute the following on the command line:
```
mvn -Dmaven.test.skip=true install
```
This will create the jars in the "target" directory of the workspace; this will also skip the unit tests.

To download the dependencies, execute this:
```
mvn dependency:copy-dependencies
```
This will download all the dependencies into the "target" directory under "dependency". The next command will package everything up:
```
mvn -Dmaven.test.skip=true -Dmaven.buildNumber.doUpdate=false package
```
Right now, this will skip the demos directory but I'm working on a fix. The xml nodes to copy the demos are in the
```
trunk/src/main/server/assembly/server.xml
```
and may be uncommented for a package build, if you have the entire svn tree checked out.

To manually copy the "demos", go to http://red5.googlecode.com/svn/flash/trunk/deploy/ and collect all the files therein. Create a directory in your red5 install at this location 
```
red5/webapps/root/demos
```
Place all the files there.

Eclipse
----------

1. Create the eclipse project files, execute this within red5-server directory.
```
mvn eclipse:eclipse
```
2. Import the project into Eclipse.
3. Access the right-click menu and select "Configure" and then "Convert to Maven Project".
4. Now the project will build automatically, if you have the maven plugin installed.

[http://screencast.com/t/2sgjMevf9 Screencast]

Donations
-------------
Donate to the cause using Bitcoin: https://coinbase.com/checkouts/2c5f023d24b12245d17f8ff8afe794d3

<i>Donations are used for beer and snacks</i>
