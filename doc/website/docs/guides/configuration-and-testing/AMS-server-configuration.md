# AMS server configuration

The Ant Media Server configurations can be made directly from the files as well as through the management console. The configurations file is more detailed.

### Settings File Under Server Configuration Folder

These settings are set stored in the file ```<AMS_DIR>`/conf/red5.properties```.

The table below summarises the available Ant Media Server settings.

*   ```policy.host```: This is a Socket Policy host. Default value is ```0.0.0.0```
*   ```policy.port```: This is a policy port. If you use this, the port should open. Default value is ```843```
*   ```server.name```: This is an Ant Media Server name. No need to fill.
*   ```server.licence_key```: This is an Ant Media Server License key. If you are using Enterprise Edition, it needs to be filled.
*   ```server.heartbeatEnabled```: This is heartbeat of Ant Media Server. Default value is ```true```
*   ```server.kafka_brokers```: This is Kafka Broker's value in Ant Media Server. Kafka default port is ```9092```. No need to fill.
*   ```server.cpu_limit```: Ant Media Server CPU Limit is based on percentage. Default value is ```75```
*   ```server.min_free_ram```: Ant Media Server free memory always must be higher than the below size. It's in MB type. Default value is ```10```
*   ```logLevel```: Ant Media Server values are TRACE, DEBUG, INFO, WARN, ERROR. Default value is ```INFO```
*   ```useGlobalIp```: The Global IP address to use when WebSocket cluster. Default value is ```false```

### HTTP Specifications

*   ```http.host```: Ant Media Server HTTP host value. No need to change. Default value is ```0.0.0.0```
*   ```http.port```: Ant Media Server HTTP port value. Default value is ```5080```
*   ```https.port```: Ant Media Server HTTPS port value. Default value is ```5443```
*   ```http.URIEncoding```: HTTP URI Encode value. Default value is ```UTF-8```
*   ```http.max_keep_alive_requests```: HTTP request max keep alive value. Default value is ```-1```
*   ```http.max_threads```: HTTP Max Threads value. Default value is ```20```
*   ```http.acceptor_thread_count```: HTTP Acceptor Thread Count value. Default value is ```10```
*   ```http.processor_cache```: HTTP Processor Cache value. Default value is ```20```

### RTMP Specifications

*   ```rtmp.host```: Ant Media Server RTMP host value. No need to change. Default value is ```0.0.0.0```
*   ```rtmp.port```: Ant Media Server RTMP Port value. Default value is ```1935```
*   ```rtmp.io_threads```: RTMP IO Threads value. Default value is ```16```
*   ```rtmp.send_buffer_size```: RTMP Send Buffer Size value. Default value is ```65536```
*   ```rtmp.receive_buffer_size```: RTMP Receive Buffer Size value. Default value is ```65536```
*   ```rtmp.ping_interval```: RTMP Ping Interval value. Default value is ```1000```
*   ```rtmp.max_inactivity```: RTMP Max Inactivity value. Default value is ```60000```
*   ```rtmp.max_handshake_time```: RTMP Max Handshake Time value. Default value is ```5000```
*   ```rtmp.tcp_nodelay```: RTMP TCP No Delay value. Default value is ```true```
*   ```rtmp.tcp_keepalive```: RTMP TCP Keep-Alive value. Default value is ```false```
*   ```rtmp.default_server_bandwidth```: RTMP Default Server Bandwidth value. Default value is ```10000000```
*   ```rtmp.default_client_bandwidth```: RTMP Default Client Bandwidth value. Default value is ```10000000```
*   ```rtmp.client_bandwidth_limit_type```: RTMP Client Bandwidth Limit Type value. Default value is ```2```
*   ```rtmp.bandwidth_detection```: RTMP Bandwidth Detection value. Default value is ```false```
*   ```rtmp.encoder_base_tolerance```: RTMP Encoder Base Tolerance value. Default value is ```5000```
*   ```rtmp.encoder_drop_live_future```: RTMP Encoder Drop Live Future value. Default value is ```false```
*   ```rtmp.traffic_class```: RTMP Traffic Class value. Default value is ```-1```
*   ```rtmp.backlog```: RTMP Backlog value. Default value is ```32```
*   ```rtmp.thoughput_calc_interval```: RTMP Thoughput Calculate Interval value. Default value is ```15```
*   ```rtmp.default_acceptor```: RTMP Default Acceptor value. Default value is ```true```
*   ```rtmp.initial_pool_size```: RTMP Initial Pool Size value. Default value is ```0```
*   ```rtmp.max_pool_size```: RTMP Max Pool Size value. Default value is ```2```
*   ```rtmp.max_processor_pool_size```: RTMP Max Processor Pool Size value. Default value is ```16```
*   ```rtmp.executor_keepalive_time```: RTMP Executer Keep-Alive Time value. Default value is ```60000```
*   ```mina.logfilter.enable```: Mina Log Filter Enable value. Default value is ```false```
*   ```rtmp.scheduler.pool_size```: RTMP Scheduler Pool Size value. Default value is ```16```
*   ```rtmp.deadlockguard.sheduler.pool_size```: RTMP Deadlockguard Scheduler Pool Size value. Default value is ```16```
*   ```rtmp.executor.core_pool_size```: RTMP Executer Core Pool Size value. RTMP Default value is ```4```
*   ```rtmp.executor.max_pool_size```: RTMP Executer Max Pool Size value. Default value is ```32```
*   ```rtmp.executor.queue_capacity```: RTMP Executer Queue Capacity value. Default value is ```64```
*   ```rtmp.executor.queue_size_to_drop_audio_packets```: RTMP Executer Queue Size to Drop Audio Packets value. Default value is ```60```
*   ```rtmp.max_handling_time```: RTMP Max Handling Time value. Default value is ```2000```
*   ```rtmp.channel.initial.capacity```: RTMP Channel Initial Capacity value. Default value is ```3```
*   ```rtmp.channel.concurrency.level```: RTMP Channel Concurrency Level value. Default value is ```1```
*   ```rtmp.stream.initial.capacity```: RTMP Stream Initial Capacity value. Default value is ```1```
*   ```rtmp.stream.concurrency.level```: RTMP Stream Concurrency Level value. Default value is ```1```
*   ```rtmp.pending.calls.initial.capacity```: RTMP Pending Calls Initial Capacity value. Default value is ```3```
*   ```rtmp.pending.calls.concurrency.level```: RTMP Pending Calls Concurrency Capacity Level value. Default value is ```1```
*   ```rtmp.reserved.streams.initial.capacity```: RTMP Reserved Streams Initial Capacity value. Default value is ```1```
*   ```rtmp.reserved.streams.concurrency.level```: RTMP Reserved Streams Concurrency Level value. Default value is ```1```
*   ```rtmp.max_packet_size```: RTMP Max Packet Size value. Default value is ```3145728```

### RTMPS Specifications

*   ```rtmps.host```: Ant Media Server RTMPS Host value. If you don't use this feature, no need to change. Default value is ```0.0.0.0```
*   ```rtmps.port```: Ant Media Server RTMP Port value. Default value is ```8443```
*   ```rtmps.ping_interval```: RTMPS Ping Interval value. Default value is ```5000```
*   ```rtmps.max_inactivity```: RTMPS Max Inactivity value. Default value is ```60000```
*   ```rtmps.max_keep_alive_requests```: RTMPS Max Keep Alive Requests value. Default value is ```-1```
*   ```rtmps.max_threads```: RTMPS Max Threads value. Default value is ```20```
*   ```rtmps.acceptor_thread_count```: RTMPS Acceptor Thread Count value. Default value is ```2```
*   ```rtmps.processor_cache```: RTMPS Processor Cache value. Default value is ```20```

### RTMPS Key and Trust store parameters

*   ```rtmps.keystorepass```: RTMPS Keystore Pass value. Default value is ```password```
*   ```rtmps.keystorefile```: RTMPS Keystore File location value. Default value is ```conf/keystore.jks```
*   ```rtmps.truststorepass```: RTMPS Truststore Pass value. Default value is ```password```
*   ```rtmps.truststorefile```: RTMPS Truststore File location value. Default value is ```conf/truststore.jks```

### RTMPT Specifications

*   ```rtmpt.host```: Ant Media Server RTMP Host value. If you don't use this feature, no need to change. Default value is ```0.0.0.0```
*   ```rtmpt.port```: Ant Media Server RTMPT Port value. Default value is ```8088```
*   ```rtmpt.ping_interval```: RTMPT Ping interval value. Default value is ```5000```
*   ```rtmpt.max_inactivity```: RTMPT Max Inactivity value. Default value is ```60000```
*   ```rtmpt.max_handshake_time```: RTMPT Max Handshake Time value. Default value is ```5000```
*   ```rtmpt.max_keep_alive_requests```: RTMPT Max Keep Alive Requests value. Default value is ```-1```
*   ```rtmpt.max_threads```: RTMPT Max Threads value. Default value is ```20```
*   ```rtmpt.acceptor_thread_count```: RTMPT Acceptor Thread Count value. Default value is ```2```
*   ```rtmpt.processor_cache```: RTMPT Processor Cache value. Default value is ```20```
*   ```rtmpt.encoder_base_tolerance```: RTMPT Encoder Base Tolerance value. Default value is ```5000```
*   ```rtmpt.encoder_drop_live_future```: RTMPT Encoder Drop Live Future value. Default value is ```true```
*   ```rtmpt.target_response_size```: RTMPT Target Response Size value. Better setting for streaming media default value is ```32768```. Best setting for small messages or shared objects default value is ```8192```
*   ```rtmpt.max_in_msg_process```: RTMPT Max Incoming Messages to Process at a time. The most that FP appears to send is ```166```.Default value is ```166```
*   ```rtmpt.max_queue_offer_time```: RTMPT Max Queue Offer Time in Millis that we will wait when offering data to the in or out queue. Default value is ```125```
*   ```rtmpt.max_queue_offer_attempts```: RTMPT Max Queue Offer Attempts value. Default value is ```4```

### Debug Proxy Specifications (needs to be activated in red5-core.xml)

*   ```proxy.source_host```: Ant Media Server Proxy Source Host value. Default value is ```127.0.0.1```
*   ```proxy.source_port```: Ant Media Server Proxy Source Port value. Default value is ```1936```
*   ```proxy.destination_host```: Proxy Destination Host value. Default value is ```127.0.0.1```
*   ```proxy.destination_port```: Proxy Destination Port value. Default value is ```1935```

### JMX Specifications

*   ```jmx.rmi.host```: Ant Media Server JMX RMI Host value. Default value is ```localhost```
*   ```jmx.rmi.port```: Ant Media Server JMX RMI Port value. Default value is ```9999```
*   ```jmx.rmi.sport```: JMX RMI Sport value. Default value is ```9998```
*   ```jmx.rmi.port.remoteobjects```: JMX RMI Port Remote Objects value. No need to fill.
*   ```jmx.keystorepass```: JMX Keystore Pass value. Default value is ```password```
*   ```jmx.mina.monitor.enable```: JMX Mina Monitor Enable value. Default value is ```false```
*   ```jmx.mina.poll.interval```: JMX Mina Poll Interval value. Default value is ```1000```
*   ```jmx.registry.create```: Whether to always create the registry in-process, not attempting to locate an existing registry at the specified port. Set to ```true``` in order to avoid the overhead of locating an existing registry when you always intend to create a new registry in any case. Default value is ```true```
*   ```jmx.reuse.existing.server```: Whether or not the ```MBeanServerFactoryBean``` should attempt to locate a running ```MBeanServer``` before creating one. Default value is ```true```
*   ```jmx.register.factory```: Whether to register the ```MBeanServer``` with the ```MBeanServerFactory```, making it available through ```MBeanServerFactory.findMBeanServer()```.Default value is ```true```
*   ```jmx.daemon```: Whether any threads started for the ```JMXConnectorServer```. Should be started as daemon threads. Default value is ```true```
*   ```jmx.threaded```: Whether the ```JMXConnectorServer``` should be started in a separate thread. Default value is ```true```

### Server Specifications

*   ```so.max.events.per.update```: Max events to send in a single update. Default value is ```64```
*   ```so.scheduler.pool_size```: Scheduler Pool size value. Default value is ```4```
*   ```keyframe.cache.entry.max```: Keyframe Cache Entry Max value. Default value is ```500```
*   ```war.deploy.server.check.interval```: War Deploy Server Check Interval value. Default value is ```600000```
*   ```fileconsumer.delayed.write```: File Consumer Delayed Write value. Default value is ```true```
*   ```fileconsumer.queue.size```: File Consumer Queue Size value. Default value is ```120```
*   ```subscriberstream.buffer.check.interval```: Stream Subscriber Buffer Check Interval value. Default value is ```5000```
*   ```subscriberstream.underrun.trigger```: Stream Subscriber Underrun Trigger value. Default value is ```100```
*   ```broadcaststream.auto.record```: Broadcast Stream Auto Record value. Default value is ```false```

### MP4 and HLS Auto Record Specifications

*   ```broadcastream.auto.record.mp4```: Broadcast Stream Auto Record MP4 value. Default value is ```true```
*   ```broadcastream.auto.record.hls```: Broadcast Stream Auto Record HLS value. Default value is ```true```

### Cluster DB Specifications

*   ```clusterdb.host```: If you don't use cluster, no need to change. Default value is ```localhost```
*   ```clusterdb.user```: If you don't use cluster, no need to change. No need to fill
*   ```clusterdb.password```: If you don't use cluster, no need to change. No need to fill