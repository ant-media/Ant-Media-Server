---
title: Alibaba
sidebar_position: 8
---

In this document, we’re going to explain how to setup a Scalable Ant Media Server Cluster in Alibaba.

Here below is the diagram about how Ant Media Server is architecturized within Alibaba.  
![image.png](@site/static/img/image(6).png)

Lets start with brief definitions

**MongoDB Database Server**: Ant Media Server uses MongoDB in clustering. Streams information are saved to MongoDB so that edge instances can learn any stream’s origin node.  
**Load Balancer**: LB is the entrance point for the publishers and players. Load Balancer accepts the requests from publishers or players and forwards the requests to the available node in the cluster.  
**Origin Auto-Scalable Group**: Nodes(Instances) in the origin group accepts the publish requests and ingest the incoming WebRTC stream. When an origin instance accepts a WebRTC stream, it saves the related information to the MongoDB Database Server. There may be one node or multiple node in origin group. It may even be manually or auto scalable. In our deployment, it’s auto-scalable in Alibaba.  
**Edge Auto-Scalable Grou**p: Node(Instances) in the edge group accepts the play requests. Then it learns from MongoDB which origin node has the related stream. After that it gets the stream from related origin node and sends the stream to the player.  
Then continue with installing MongoDB Server

## Step 1: Install MongoDB Server

The procedure below shows how to start an instance in Alibaba Elastic Compute Service as well. In other words, if you have no experience with Alibaba, you can even install MongoDB Server as follows. If you know how to start an instance in Alibaba, just skip to “Install MongoDB to Your Instance”

[Signup](https://www.alibabacloud.com/) to Alibaba Cloud if you don’t have an account yet. Login to Alibaba Cloud Console. Then click Elastic Compute Service as shown in the image below.  
![image.png](@site/static/img/image(7).png)

-   Click Create Instance  
    ![image.png](@site/static/img/image(8).png)
    
-   Select Billing model and Instance type according to your use case.
    

![image.png](@site/static/img/image(9).png)

-   Choose Instance Type like Enhanced General Purpose Type g6e series
    
-   -   You may optionally choose a bigger instance according to your streaming load.  
        Search for Ubuntu and Select Ubuntu 18.04 and Click Next button  
        ![image.png](@site/static/img/image(10).png)
-   Create a VPC and security group  
    ![image.png](@site/static/img/image(11).png)
    

You need to have VPC and security groups for creating MongoDB instance. If you already have VPC and Security Group options, you can skip Create VPC and Create Security Group parts.

### Create VPC

-   Go to Elastic Compute Service / Network & Security / Virtual Private Cloud section and click Create VPC button.

![image.png](@site/static/img/image(12).png)

-   Fill Name and IPv4 CIDR Block parameters.  
    ![image.png](@site/static/img/image(13).png)
    
-   You need to have 2 vSwitch for loadbalancing mechanism. Fill vSwitch Name, Zone and IPv4 CIDR Block parameters.  
    ![image.png](@site/static/img/image(14).png)
    

### Create Security Group

-   Go to Elastic Compute Service / Network & Security / Security Groups section and click Create Security Group button.  
    ![image.png](@site/static/img/image(15).png)
    
-   Add 22 and 27017 TCP ports as follows in the image.  
    ![image.png](@site/static/img/image(16).png)
    
-   Go back to creating instance steps.  
    ![image.png](@site/static/img/image(17).png)
    
-   You need to create an SSH key. If you already created an SSH key, you can skip SSH key step.  
    ![image.png](@site/static/img/image(18).png)
    
-   After creating the SSH key, just click "Create Instance" button  
    ![image.png](@site/static/img/image(19).png)
    
-   Login your Instance according to Instance public IP address with a created SSH key.
    
-   Right now, you should connect to your instance. To Connect your instance, open a terminal and run a command something like. Please change {YOUR_KEY_FILE} and {INSTANCE_PUBLIC_IP} with your own credentials.
    

```
ssh -i {YOUR_KEY_FILE} root@{INSTANCE_PUBLIC_IP}

```

### Install MongoDB to Your Instance

-   After you get connected, run the following commands in order to install MongoDB to your instance

```
wget -qO - https://www.mongodb.org/static/pgp/server-4.2.asc | sudo apt-key add -
echo "deb [ arch=amd64 ] https://repo.mongodb.org/apt/ubuntu `lsb_release -cs`/mongodb-org/4.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.2.list
sudo apt-get update
sudo apt-get install -y mongodb-org
```

Open /etc/mongod.conf file with an editor like nano and change bind_ip value to 0.0.0.0 to let MongoDB accept connections in all interfaces and save it.

```
sudo nano /etc/mongod.conf
```

![image.png](@site/static/img/image(20).png)

Press Ctrl + X to save the file.

Restart mongod and enable service.

```
sudo systemctl restart mongod
sudo systemctl enable mongod.service
```

MongoDB installation is complete, just save your MongoDB instance’s local address somewhere. We will use it in later.

### Step 2: Install Scalable Origin Group

-   Click "Deployment & Elasticity > Auto Scaling" and Click "Create" button.  
    ![image.png](@site/static/img/image(21).png)

You need to have a Launch template for creating Instances. If you already have the Launch template, you can skip Create a Launch Template parts.

### Create a Launch Template

-   Click "Deployment & Elasticity > Launch Template" and Click "Create Template" button.  
    ![image.png](@site/static/img/image(22).png)
    
-   Select Ant Media Marketplace image, VPC network and click Assign Public IPv4 Address as follows.  
    ![image.png](@site/static/img/image(23).png)
    
-   Select Ant Media Server Enterprise Edition as follows.  
    ![image.png](@site/static/img/image(24).png)
    
-   Select/create Security Group as follows  
    ![image.png](@site/static/img/image(25).png)
    
-   Select VPC Network and click Next Advanced Configurations  
    ![image.png](@site/static/img/image(27).png)
    
-   Add SSH key pair, Instance name and User Data details as follows. Right now, copy the text below, change the "{MongoIP}" field with the MongoDB IP Address in the script and paste it to the "User data".
    

```
#!/bin/bash
cd /usr/local/antmedia
./change_server_mode.sh cluster {MongoIP}
```

After that Click "Confirm Complete Configuration"  
![image.png](@site/static/img/image(28).png)

-   Take a look at Launch Template and click Confirm Template Configuration  
    ![image.png](@site/static/img/image(29).png)
    
-   Go back to Create Scaling Group steps and use created Launch Template.  
    ![image.png](@site/static/img/image(30).png)
    
-   Set Minimum Number of Instances, Maximum Number of Instances and Expected Number of Instances and Default Cooldown Time (Seconds) as follows  
    ![image.png](@site/static/img/image(31).png)
    
-   Set VPC, Select vSwitch and Create Associated ALB Server Group  
    ![image.png](@site/static/img/image(32).png)
    
-   Set Server Group Name and VPC fields as follows  
    ![image.png](@site/static/img/image(33).png)
    
-   Go back to Create Scaling Group steps and select created Server Group as follows  
    ![image.png](@site/static/img/image(34).png)
    
-   Click Enable the scaling group text as follows  
    ![image.png](@site/static/img/image(35).png)
    
-   Enable Autoscaling as follows  
    ![image.png](@site/static/img/image(36).png)
    

### Step 3: Install Scalable Edge Group

Installing scalable edge group almost same as scalable origin group. Please go to Step 2: Install Scalable Origin Group and do the same things one more time. Just don’t forget to change some namings(for instance give group name as Edge Group) and configure scaling policy and instance type according to your needs. If you have any question or problem with this, please let us know through contact@antmedia.io

### Step 4: Install Load Balancer

-   Click the "Server Load Balancer > ALB > Instances" on Alibaba Products and Services and Click the "Create ALB Instance" button as follows  
    ![image.png](@site/static/img/image(37).png)
    
-   Fill the fields as follows  
    ![image.png](@site/static/img/image(38).png)
    
-   You need to add listeners to load balancers as follows  
    ![image.png](@site/static/img/image(39).png)
    
-   You need to have SSL. But no need to buy certificate. Just click Buy Certificate text.  
    ![image.png](@site/static/img/image(40).png)
    
-   After click Buy Certificate button, just click Upload Certificate button and upload your custom certificate. You can get free let's encrypt SSL with https://zerossl.com service.  
    ![image.png](@site/static/img/image(41).png)
    
-   Just add created Server group  
    ![image.png](@site/static/img/image(42).png)
    
-   Add listeners ports to origin/edge groups  
    ![image.png](@site/static/img/image(43).png)
    
-   Right now Everything is ok. Just let me give a brief information about the difference between publish and play. In our load balancer configuration, we forward HTTP(80) and HTTPS(443) to Origin Group and we forward HTTP(5080) and HTTPS(5443) to Edge Group. It means that we should connect 80 or 443 ports to publish and connect 5080 or 5443 to play streams. Otherwise, play requests goes to origin group and publish request goes to edge group and it’s likely create some performance issues according to your configurations.
    

### Logging in Web Panel

You can login to web panel via the https://your-domain-name/. After these steps, you just need to add your domain address CNAME to your Load Balancer URL

#### Test Fly

-   For publishing please visit the https://your-domain-name/WebRTCAppEE/ and click “Start Publishing” button. The default stream id is “stream1”
    
-   For playing please visit the https://your-domain-name:5443/WebRTCAppEE/ and click “Start Playing” button. The default stream will be played
    
-   As you figure out, we connect default https port(443) for publishing and 5443 port for playing. Because we configure load balancer to forward default port(443) to origin group and 5443 to edge group.
    

Markdown Shortcut

1238 words

Published 28 Mar 2022