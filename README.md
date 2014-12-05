red5-server
===========

Red5 server core

Some of the Red5 project information continues to reside on Google Code. Post new issues or wiki entries here. https://code.google.com/p/red5/

The Red5 users list may be found here: https://groups.google.com/forum/#!forum/red5interest

Subreddit: http://www.reddit.com/r/red5

Jenkins builds: https://builds.apache.org/view/M-R/view/OpenMeetings/job/Red5-server/ (Courtesy of Apache / OpenMeetings)

Latest version is <b>1.0.4-SNAPSHOT</b>

Current Releases
----------------

<h4>Red5 1.0.3 Release (5 August 2014)</h4>
<a href="https://mega.co.nz/#!JcNwyKab!Mk7aMIL_bAsRQgReBz0hIuDBs0vfncl901ZtLY3u0dM">Tarball</a> | 
<a href="https://mega.co.nz/#!1BdigLgY!m_mGoSw5SEPh0Nf8qGGmGaUn3VXpIVrYHpVYCYNsK7Q">ZIP</a>

<h4>Red5 1.0.2 Release (9 April 2014)</h4>
<a href="https://mega.co.nz/#!FFsV0TIC!eEeGePK30nCv0xF5E7w_6S3b_z8Y9pjzMkp2-UgZTYk">Windows Java7</a> | 
<a href="https://mega.co.nz/#!8EUFwAxR!qJjgtFCs5tY86ZDqolL_nL9SsWradm4BQeOugffZqqs">Tarball</a> | 
<a href="https://mega.co.nz/#!QUNEiDoI!RhT8p660eJImIuI3kRhuZHfRWxtnZTSpp0-va2_wyrw">ZIP</a>

<i>Scroll to the bottom of this page for older releases</i>

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

Previous Releases
-------------------

<h4>Red5 1.0.2 Snapshot (18 April 2013)</h4>

<a href="https://mega.co.nz/#!5M0zAKyZ!EajiiQUjjr9N6Lcpi2NTG2JY-e4owoGaUy5ilqxc6Fc">Windows</a> | 
<a href="https://mega.co.nz/#!gYV0TBjY!H-1RSkcRHbQ-OBFJVXuVkmPIdt8LdZ5cM4OigHRZNE0">ZIP</a> 

<h4>Red5 1.0.1 Final (14 January 2013)</h4>

<a href="http://red5.org/downloads/red5/1_0_1/setup-Red5-1.0.1.exe">Windows</a> _Java 7_ | 
<a href="http://red5.org/downloads/red5/1_0_1/setup-Red5-1.0.1-java6.exe">Windows</a> _Java 6_ | <a href="http://red5.org/downloads/red5/1_0_1/red5-1.0.1.zip">ZIP</a> | 
<a href="http://red5.org/downloads/red5/1_0_1/red5-1.0.1.tar.gz">Tarball</a>

<h4>Red5 1.0 Final (03 December 2012)</h4>

<a href="http://red5.org/downloads/red5/1_0/setup-Red5-1.0.0.exe">Windows</a> _Java 7_ | 
<a href="http://red5.org/downloads/red5/1_0/setup-Red5-1.0.0-java6.exe">Windows</a> _Java 6_ | <a href="http://red5.org/downloads/red5/1_0/red5-1.0.0.zip">ZIP</a> |
<a href="http://red5.org/downloads/red5/1_0/red5-1.0.0.tar.gz">Tarball</a>

<h4>Red5 0.9.1 Final (21 February 2010)</h4>

<a href="http://red5.org/downloads/red5/0_9/red5-0.9.1.dmg">OSX</a> | 
<a href="http://red5.org/downloads/red5/0_9/setup-Red5-0.9.1.exe">Windows</a> | 
<a href="http://red5.org/downloads/red5/0_9/red5-0.9.1.zip">ZIP</a> | 
<a href="http://red5.org/downloads/red5/0_9/red5-0.9.1.tar.gz">Tarball</a> | 
<a href="http://red5.org/downloads/red5/0_9/red5-0.9.1.jar">Replacement Jar</a>

<h4>Red5 0.9.0 Final (27 January 2010)</h4>

<a href="http://red5.org/downloads/red5/0_9/red5-0.9.0.dmg">OSX</a> | 
<a href="http://red5.org/downloads/red5/0_9/setup-Red5-0.9.0.exe">Windows</a> | 
<a href="http://red5.org/downloads/red5/0_9/red5-0.9.0.zip">ZIP</a> | 
<a href="http://red5.org/downloads/red5/0_9/red5-0.9.0.tar.gz">Tarball</a> | 
<a href="http://red5.org/downloads/red5/0_9/red5-src-0.9.0.zip">Source</a>

<h4>Red5 0.8.0 Final</h4>

<a href="http://red5.org/downloads/red5/0_8/setup-red5-0.8.0.dmg">OSX</a> | 
<a href="http://red5.org/downloads/red5/0_8/setup-Red5-0.8.0.exe">Windows</a> | 
<a href="http://red5.org/downloads/red5/0_8/setup-Red5-0.8.0-java5.exe">Windows</a> (Java5) | 
<a href="http://red5.org/downloads/red5/0_8/red5-0.8.0.tar.gz">Tarball</a> | 
<a href="http://red5.org/downloads/red5/0_8/red5-0.8.0-java5.tar.gz">Tarball</a> (Java5) | 
<a href="http://red5.org/downloads/red5/0_8/red5-war-0.8.0.zip">WAR</a> | 
<a href="http://red5.org/downloads/red5/0_8/red5-war-0.8.0-java5.zip">WAR</a> (Java5)

Donations
-------------
Donate to the cause using Bitcoin: https://coinbase.com/checkouts/2c5f023d24b12245d17f8ff8afe794d3

<i>Donations are used for beer and snacks</i>
