red5-server - Red5 server core
===========

Red5 is an Open Source Flash Server written in Java that supports:

 * Streaming Video (FLV, F4V, MP4, 3GP)
 * Streaming Audio (MP3, F4A, M4A, AAC)
 * Recording Client Streams (FLV and AVC+AAC in FLV container)
 * Shared Objects
 * Live Stream Publishing
 * Remoting
 * Protocols: RTMP, RTMPT, RTMPS, and RTMPE
  
Support via plugin:
 
 * [WebSocket (ws and wss)](https://github.com/Red5/red5-websocket)
 * [HLS](https://github.com/Red5/red5-hls-plugin)
 * [RTSP (From Axis-type cameras)](https://github.com/Red5/red5-rtsp-restreamer)

The Red5 users list may be found here: https://groups.google.com/forum/#!forum/red5interest

Subreddit: http://www.reddit.com/r/red5

Automatic builds (Courtesy of Apache / OpenMeetings): 
 * [Red5](https://builds.apache.org/view/M-R/view/OpenMeetings/job/Red5-server/)
 * [Windows Installer](https://builds.apache.org/view/M-R/view/OpenMeetings/job/red5-installer/)

Current version is <b>1.0.6-RELEASE</b>

[Latest Releases](https://github.com/Red5/red5-server/releases/latest)
----------------
<h4>Red5 1.0.6 Release (8 September 2015)</h4>
[Tarball &amp; ZIP](https://github.com/Red5/red5-server/releases/tag/v1.0.6-RELEASE)

<h4>Red5 1.0.5 Release (7 February 2015)</h4>
[Tarball &amp; ZIP](https://github.com/Red5/red5-server/releases/tag/v1.0.5-RELEASE)

<h4>Red5 1.0.4 Release (26 December 2014)</h4>
[Tarball](https://github.com/Red5/red5-server/releases/download/v1.0.4-RELEASE/red5-server-1.0.4-RELEASE-server.tar.gz) | [ZIP](https://github.com/Red5/red5-server/releases/download/v1.0.4-RELEASE/red5-server-1.0.4-RELEASE-server.zip)

[Previous releases](https://github.com/Red5/red5-server/blob/master/README.md#previous-releases)

<i>Note on Bootstrap</i>

The bootstrap and shutdown classes have been moved to the red5-service project; the dependency has been added to this projects pom.

Maven Artifacts
-----------------

Releases are available at https://oss.sonatype.org/content/repositories/releases/org/red5/

Snapshots are available at https://oss.sonatype.org/content/repositories/snapshots/org/red5/

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

To package everything up in an assembly (tarball/zip):
```
mvn -Dmaven.test.skip=true clean package -P assemble
```
To build a milestone tarball:
```sh
mvn -Dmilestone.version=1.0.7-M1 clean package -Pmilestone
```

Eclipse
----------

1. Create the eclipse project files, execute this within red5-server directory.
```
mvn eclipse:eclipse
```
2. Import the project into Eclipse.
3. Access the right-click menu and select "Configure" and then "Convert to Maven Project".
4. Now the project will build automatically, if you have the maven plugin installed.

[Screencast](http://screencast.com/t/2sgjMevf9)

Previous Releases
-------------------

The artifacts for the following releases are no longer available; if your project requires them, you'll have to build them from source. The listings are here only for historical purposes.

<ul>
<li>Red5 1.0.3 Release (5 August 2014)</li>
<li>Red5 1.0.2 Release (9 April 2014)</li>
<li>Red5 1.0.2 Snapshot (18 April 2013)</li>
<li>Red5 1.0.1 Final (14 January 2013)</li>
<li>Red5 1.0 Final (03 December 2012)</li>
<li>Red5 0.9.1 Final (21 February 2010)</li>
<li>Red5 0.9.0 Final (27 January 2010)</li>
<li>Red5 0.8.0 Final</li>
</ul>

Supporters
-------------
[Powerflasher for FDT](http://fdt.powerflasher.com/)

[YourKit](http://www.yourkit.com/) YourKit is kindly supporting open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications. Take a look at YourKit's leading software products:

[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp)   

[YourKit .NET Profiler](http://www.yourkit.com/dotnet/index.jsp)


Donations
-------------
Donate to the cause using Bitcoin: https://coinbase.com/checkouts/2c5f023d24b12245d17f8ff8afe794d3

<i>Donations are used for beer and snacks</i>
