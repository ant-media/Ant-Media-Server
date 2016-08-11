red5-server - Red5 server core
===========

![TravisCI](https://travis-ci.org/Red5/red5-server.svg?branch=master) 
[![Maven Central](https://img.shields.io/maven-central/v/org.red5/red5-server.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.red5%22)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)

Red5 is an Open Source Flash Server written in Java that supports:

 * Streaming Video (FLV, F4V, MP4, 3GP)
 * Streaming Audio (MP3, F4A, M4A, AAC)
 * Recording Client Streams (FLV and AVC+AAC in FLV container)
 * Shared Objects
 * Live Stream Publishing
 * Remoting
 * Protocols: RTMP, RTMPT, RTMPS, and RTMPE
  
Additional features supported via plugin:
 
 * [WebSocket (ws and wss)](https://github.com/Red5/red5-websocket)
 * [RTSP (From Axis-type cameras)](https://github.com/Red5/red5-rtsp-restreamer)
 * [HLS](https://github.com/Red5/red5-hls-plugin)

The Red5 users list may be found here: [red5interest](https://groups.google.com/forum/#!forum/red5interest)

Subreddit: [r/red5](http://www.reddit.com/r/red5)

Automatic builds (Courtesy of Apache [OpenMeetings](http://openmeetings.apache.org/)): 
 * [Red5](https://builds.apache.org/view/M-R/view/OpenMeetings/job/Red5-server/)
 * [Windows Installer](https://builds.apache.org/view/M-R/view/OpenMeetings/job/red5-installer/)

# Releases
[Releases](https://github.com/Red5/red5-server/releases/latest)
----------------
### Red5 1.0.7 Release (12 May 2016)
[Tarball &amp; ZIP](https://github.com/Red5/red5-server/releases/tag/v1.0.7-RELEASE)

### Red5 1.0.6 Release (8 September 2015)
[Tarball &amp; ZIP](https://github.com/Red5/red5-server/releases/tag/v1.0.6-RELEASE)

## Previous
[Previous releases](https://github.com/Red5/red5-server/blob/master/README.md#previous-releases)

<i>Note on Bootstrap</i>

The bootstrap and shutdown classes have been moved to the [red5-service](https://github.com/Red5/red5-service) project; the dependency has been added to this projects pom.

# StackOverflow
If you want answers from a broader audience, [Stack Overflow](http://stackoverflow.com/tags/red5/info) may be your best bet.

# Maven
Releases are available at [Sonatype - Releases](https://oss.sonatype.org/content/repositories/releases/org/red5/)

Snapshots are available at [Sonatype - Snapshots](https://oss.sonatype.org/content/repositories/snapshots/org/red5/)

Include the red5-parent in your __pom.xml__  in the __dependencyManagement__ section
```xml
<dependencyManagement>
    <dependencies>
      <dependency>
          <groupId>org.red5</groupId>
          <artifactId>red5-parent</artifactId>
          <version>${red5.version}</version>
          <type>pom</type>
      </dependency>
    </dependencies>
</dependencyManagement>  
```
in addition to any other Red5 projects in the __dependencies__ section
```xml
  <dependency>
      <groupId>org.red5</groupId>
      <artifactId>red5-server</artifactId>
      <version>${red5.version}</version>
      <type>jar</type>
  </dependency>
```

## Build from Source

To build the red5 jars, execute the following on the command line:
```sh
mvn -Dmaven.test.skip=true install
```
This will create the jars in the "target" directory of the workspace; this will also skip the unit tests.

To package everything up in an assembly (tarball/zip):
```sh
mvn -Dmaven.test.skip=true clean package -P assemble
```
To build a milestone tarball:
```sh
mvn -Dmilestone.version=1.0.7-M1 clean package -Pmilestone
```

# Eclipse

1. Create the eclipse project files, execute this within red5-server directory.
```sh
mvn eclipse:eclipse
```
2. Import the project into Eclipse.
3. Access the right-click menu and select "Configure" and then "Convert to Maven Project".
4. Now the project will build automatically, if you have the maven plugin installed.

[Screencast](http://screencast.com/t/2sgjMevf9)

# Older Releases
The artifacts for the following releases are no longer available; if your project requires them, you'll have to build them from source. The listings are here only for historical purposes.

 * [Red5 1.0.5 Release](https://github.com/Red5/red5-server/releases/tag/v1.0.5-RELEASE) (7 February 2015)
 * [Red5 1.0.4 Release](https://github.com/Red5/red5-server/releases/tag/v1.0.4-RELEASE) (26 December 2014)
 * [Red5 1.0.3 Release](https://github.com/Red5/red5-server/releases/tag/v1.0.3-RELEASE) (5 August 2014)
 * Red5 1.0.2 Release (9 April 2014)
 * Red5 1.0.2 Snapshot (18 April 2013)
 * Red5 1.0.1 Final (14 January 2013)
 * Red5 1.0 Final (03 December 2012)
 * Red5 0.9.1 Final (21 February 2010)
 * Red5 0.9.0 Final (27 January 2010)
 * Red5 0.8.0 Final

### Donations
Donate to the cause using
<table>
  <tr><td>BTC</td><td>19AUgJuVzC8jg16bSLJDcM6Nfouh9JvwKA</td></tr>
  <tr><td>ETH</td><td>0x099F9DE7B799C0Dd0bE2E93Ed9BeA7CB1Fe989d2</td></tr>
</table>
<i>Donations are used for beer and snacks</i>

