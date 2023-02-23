# Load testing

In this tutorial, we'll explain how to make a load testing on your Ant Media Server. The test environment has two parts: test server and SUT (system under test). We have two different setups for two different SUTs.

Preparation of SUT
------------------

### One instance setup

In this option, we have only one Ant Media Server instance as SUT.

    +-------------------+                  +----------------------+
    |                   |   streaming      |                      |
    |                   |   playing        |                      |
    |                   | `<-------------->` |                      |
    |    Test Server    |                  |   Ant Media Server   |
    |                   |                  |                      |
    |                   | `<-------------->` |                      |
    |                   |    rest          |                      |
    +-------------------+                  +----------------------+

To setup the Ant Media Server, please check [here](https://github.com/ant-media/Ant-Media-Server/wiki/Installation).

### Cluster setup

Here we have a cluster structure as SUT which contains one origin and N edge servers.

                                           +--------------------+
                                           |                    |
                                           |                    |
                                           |  Ant Media Server  |
                                +--------->`+                    |
                                |          |     (Origin)       |
    +-----------+               |          |                    |
    |           |    streaming  |          |                    |
    |           +---------------+          +--------------------+
    |           |
    |Test Server| playing +------------------------------------------------+
    |           +`<--------+                                                |
    |           |         |             Load Balancer                      |
    |           +---------+                                                |
    +-----------+   rest  +--+------------+---------------------+----------+
                             |            |                     |
                             |            |                     |
                             |            |                     |
                             |            |                     |
                             |            |                     |
               +-------------+--+  +------+---------+     +-----+----------+
               |                |  |                |     |                |
               |                |  |                |     |                |
               |Ant Media Server|  |Ant Media Server| ... |Ant Media Server|
               |                |  |                |     |                |
               |   (Edge-1)     |  |   (Edge-2)     |     |   (Edge-N)     |
               |                |  |                |     |                |
               |                |  |                |     |                |
               +----------------+  +----------------+     +----------------+
    

To deploy an Ant Media Server cluster, please see [here](https://github.com/ant-media/Ant-Media-Server/wiki/Scaling-and-Load-Balancing).

Ant Media WebRTC test tool
--------------------------

You can download the WebRTC load test tool from your account at [antmedia.io](https://antmedia.io/). The test tool is listed with Enterprise Edition under Download section of your Subscription.

Ant Media WebRTC Test Tool is a Java project for testing Ant Media Server WebRTC capabilities and has the following features.

*   This tool is compatible with Ant Media Server signaling protocol.
*   It has two modes: publisher and player (-m flag determines the mode)
*   It has two options with UI or without UI (-u flag determines the UI on/off)
*   You can also save received (in player mode) video.
*   You can create a load with the -n flag.

Running Ant Media WebRTC Test Tool
----------------------------------

#### Installation

    apt-get install openjdk-11-jre -y
    unzip webrtctest-release-*.zip
    cd webrtctest/

 This tool can be run from the terminal with the following options.

    ./run.sh -f output.mp4 -m publisher -n 1  #publishes output.mp4 to the server with default name myStream

    ./run.sh -m player -n 100 -s 10.10.175.53 -u false #plays 100 viewers for default stream myStream

#### Parameters

    Flag 	 Name      	 Default   	 Description                 
    ---- 	 ----      	 -------   	 -----------   
    f    	 File Name 	 test.mp4     	 Source file* for publisher output file for player        
    s    	 Server IP 	 localhost 	 server ip                   
    q    	 Security  	 false     	 true(wss) or false(ws)      
    l        Log Level       3               0:VERBOSE,1:INFO,2:WARNING,3:ERROR,4:NONE
    i    	 Stream Id 	 myStream  	 id for stream               
    m    	 Mode      	 player    	 publisher or player         
    u    	 Show GUI  	 true      	 true or false               
    p    	 Port      	 5080      	 websocket port number 
    v    	 Verbose   	 false     	 true or false 
    n    	 Count     	 1         	 Number of player/publisher connctions 
    k        Kafka Broker    null            Kafka broker address with port
    r    	 Publish Loop 	 false           true or false
    c    	 Codec           h264            h264 or VP8 
    d    	 Data Channel    false           true or false 

Note that the file in mp4 format should have h264 encoded video and Opus encoded audio.