---
title: Multi Level Cluster
sidebar_position: 3
---
# Multi Level Cluster

### What is Multi-Level Cluster?

A cluster which has different regions is called Multi-Level Cluster where each region has its own origin for a stream. Ant Media Server can be scaled in different physical locations. You can set the node group parameter of the servers to create regions. Separating the nodes into regions based on their physical location has some advantages in performance and data-transfer. Because each region will have their own origin node for a stream, the edges in a region will pull the stream from the origin of their region.

### How does it work?

Let's clarify the the case with an example scenario. We have two scenarios

1.  Publisher & Players in the same cluster
2.  Publisher & Players in the different clusters

Let's remember the traditional scenario

#### Publisher & Players in the same cluster

1.  ```Publisher``` starts a stream and it is assigned to the ```Origin1``` instance in the ```Region1```
2.  ```Player1``` who is close to ```Region1``` instance requests to play the stream.
3.  ```Player1``` is assigned to ```Edge11``` instance to receive the stream in ```Region1```
4.  Since the ```Origin1``` is the origin of the stream, ```Edge11``` will pull stream from ```Origin1```.

![](@site/static/img/multilevelcluster.png)

#### Publisher & Players in the different clusters

1.  ```Player2``` who is close to ```Region2``` requests to play the stream.
2.  ```Player2``` is assigned to ```Edge21``` instance in ```Region2```
3.  ```Edge21```checks the origin of the stream.
    *   Since there is no origin instance in ```Region2``` to play the stream. It assigns itself as the secondary origin for that stream in ```Region2```.
    *   ```Edge21``` pulls the stream from the ```Origin1``` (main origin) in ```Region1```.
    *   ```Edge21``` sends the stream to ```Player2```.
4.  ```Player3``` who is close to ```Region2``` requests to play the stream.
5.  ```Player3``` is assigned to ```Edge22``` in ```Region2```.
6.  ```Edge22``` checks the origin of the stream. Since ```Edge21``` is a secondary origin for the stream in the ```Region2```, it pulls the stream from ```Edge21``` and serves to ```Player3```.

In short, each stream is distributed from single node in each region even if they are in different regions.

### How to configure Multi-Level Cluster?

You can set the node group (or region) of a server by adding ```nodeGroup=GROUP_NAME``` in ```conf/red5.properties``` file. Make sure that the instances running in the same region should have the same ```nodeGroup``` value

Keep in mind that you should configure the Load Balancer to forward Publish and Play requests to the best region. If you have a global cluster in different geolocations, you should have a Load Balancer like Route53.