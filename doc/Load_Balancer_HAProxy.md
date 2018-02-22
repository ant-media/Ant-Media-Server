# Load Balancer with HAProxy

Load Balancer is the sister of cluster so If you make Ant Media Server instances run in Cluster Mode. 
Then a load balancer will be required to balance the load to the each instance. For our case, we are going to show
how to use HAProxy as RTMP and HLS load balancer. 


## Installing HAProxy

You can install HAProxy to both one of the instances that run on Ant Media Server or you can make it run on different instance.
For our case, we assume that HAProxy runs on a specific instance. So let's install HAProxy

```
sudo apt-get install haproxy
```
Let HAProxy be started with init script
```
nano /etc/default/haproxy
```
Set `ENABLED` option to `1`
```
ENABLED=1
```

## Configuration HAProxy

Move the default configuration file
```
mv /etc/haproxy/haproxy.cfg{,.original}
```

Create and edit new configuration file
```
nano /etc/haproxy/haproxy.cfg
```

Begin with global parameters

```
global
    log 127.0.0.1 local0 notice
    maxconn 2000
    user haproxy
    group haproxy
```
The configuration above makes maximum number of connections to 2000. Please change it according to your hardware and cluster size.

Enter monitoring parameters.  
```
listen stats # Define a listen section called "stats"
  bind :6080 
  mode http
  stats enable  # Enable stats page
  stats hide-version  # Hide HAProxy version
  stats realm Haproxy\ Statistics  # Title text for popup window
  stats uri /haproxy_stats  # Stats URI
  stats auth Username:Password  # Authentication credentials
```
With the configuration above when you go to `http://HAPROXY_LB:6080/haproxy_stats` URL, you can authenticate
with `Username` and `Password` so that specify username and password.

Configure RTMP Load Balancing
```
frontend rtmp_lb
    bind *:1935 
    mode tcp
    default_backend backend_rtmp

backend backend_rtmp
    mode tcp
    server ams1 172.30.0.42:1935 check  # Ant Media Server instance 1
    server ams2 172.30.0.48:1935 check  # Ant Media Server instance 2
    # you can add more instances 
```    

Configure HTTP Load Balancing
```
frontend http_lb
    bind *:5080 
    mode tcp
    default_backend backend_http

backend backend_http
    mode tcp
    server ams1 172.30.0.42:5080 check  # Ant Media Server instance 1
    server ams2 172.30.0.48:5080 check  # Ant Media Server instance 2
    # you can add more instances 
```

When you are done configuration file should be like this

```
global
    log 127.0.0.1 local0 notice
    maxconn 2000
    user haproxy
    group haproxy

listen stats # Define a listen section called "stats"
  bind :6080 
  mode http
  stats enable  # Enable stats page
  stats hide-version  # Hide HAProxy version
  stats realm Haproxy\ Statistics  # Title text for popup window
  stats uri /haproxy_stats  # Stats URI
  stats auth Username:Password  # Authentication credentials


frontend rtmp_lb
    bind *:1935 
    mode tcp
    default_backend backend_rtmp

backend backend_rtmp
    mode tcp
    server ams1 172.30.0.42:1935 check
    server ams2 172.30.0.48:1935 check
    
frontend http_lb
    bind *:5080 
    mode tcp
    default_backend backend_http

backend backend_http
    mode tcp
    server ams1 172.30.0.42:5080 check  # Ant Media Server instance 1
    server ams2 172.30.0.48:5080 check  # Ant Media Server instance 2
    # you can add more instances     
```

## Starting HAProxy

When everything is comple restart the HAProxy
```
sudo service haproxy restart
```
and you can view status of the instance throught http://HAPROXY_LB:6080/haproxy_stats URL
![HAProxy Stats Panel](https://ant-media.github.io/Ant-Media-Server/doc/images/HAProxy_Stats.png)

If you have a question, please let us know through contact@antmedia.io

    
