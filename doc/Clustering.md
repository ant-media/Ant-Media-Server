# Clustering

Ant Media Server can run in Cluster Mode. It means several instances can transport stream to each other and can handle
high load by running together in the same network.


## Enable Clustering

Enabling clustering is very straight forward. Let's make it step by step 

* Go to the conf folder in the installation directory. Default install dir is `/usr/local/antmedia` 
```
cd /usr/local/antmedia/conf
```

* Rename jee-container.xml to jee-container-standalone.xml
```
mv jee-container.xml jee-container-standalone.xml
```

* Rename jee-container-cluster.xml to jee-container.xml
```
mv jee-container-cluster.xml jee-container.xml
```

* Restart the server
```
sudo service antmedia stop
sudo service antmedia start
```

Your server runs in cluster mode.


## How Clustering Works

* Clustering in Ant Media Server is masterless when a new instance joins the same network, 
the other instances discover new joined instance and new instance learns all other the instances in the same network.

* When an instance starts to receive live stream, it broadcast a udp message and other instances know that which instance
is receiving which stream.

* When the load balancer forwards a play request to any of the instances in the network. The instance knows where to fetch 
the live stream from. It fetches live stream and send to audience. 

* Clustering work both for HLS and RTMP live streams





