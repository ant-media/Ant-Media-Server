# Clustering with AWS

In this document, we’re going to explain how to setup a Scalable Ant Media Server Cluster in Amazon Web Services. Scaling is required when a single server cannot meet the required demand. You can also estimate your cost and server requirement through our [cost calculator](https://antmedia.io/cost-calculator/) and down the page you can see the table for supported values as per server CPU resources.

Here below is the diagram about how Ant Media Server is architecturized within AWS.

![](@site/static/img/68747470733a2f2f616e746d656469612e696f2f77702d636f6e74656e742f75706c6f6164732f323032312f30342f4157532d636c75737465722d312e706e67.png)

Lets start with brief definitions

* **MongoDB Database Server:** Ant Media Server uses MongoDB in clustering. Streams information are saved to MongoDB so that edge instances can learn any stream’s origin node.
* **Load Balancer:** LB is the entrance point for the publishers and players. Load Balancer accepts the requests from publishers or players and forwards the requests to the available node in the cluster.
* **Origin Auto-Scalable Group:** Nodes (Instances) in the origin group accepts the publish requests and ingest the incoming WebRTC stream. When an origin instance accepts a WebRTC stream, it saves the related information to the MongoDB Database Server. There may be one node or multiple node in origin group. It may even be manually or auto scalable. In our deployment, it’s auto-scalable in AWS.
* **Edge Auto-Scalable Group:** Node (Instances) in the edge group accepts the play requests. Then it learns from MongoDB which origin node has the related stream. After that it gets the stream from related origin node and sends the stream to the player.

Then continue with installing MongoDB Server.

### Step 1: Install MongoDB Server

The procedure below shows how to start an instance in AWS EC2 service as well. In other words, if you have no experience about AWS, you can even install MongoDB Server as follows. If you know how to start an instance in AWS, just skip to “Install MongoDB to Your Instance”

* [Signup](https://aws.amazon.com/) to AWS if you don’t have an account yet. Login to [AWS Management Console](https://console.aws.amazon.com/). Then click EC2 Service as shown in the image below.

![](@site/static/img/152641240-633d9b3a-179d-4b9e-b0c5-2d1e12f9c7b7.png)

* Click “Launch” Instance.

![](@site/static/img/152640815-81636a15-dc3e-44d5-8d90-4cff72cac14c(1).png)

* Search for “Ubuntu” and Select “Ubuntu 20.04”.

![](@site/static/img/152640816-06bcfe6e-35ed-4efe-9fda-bacb60826a82(1).png)

* Choose Instance Type like m4.xlarge or m5.xlarge series. There are two points here.
  * First one is you may optionally choose a bigger instance according to your streaming load.
  * Second one don’t use any m5a instances because they have ARM architecture.

Then click “Review and Launch”.

![](@site/static/img/152640818-2b08b818-fb6e-428a-a2d7-aeba6b32be51(1).png)

* Click “Configure Security Group” in the image.
* Add “22” and “27017” TCP ports as follows in the image. Warning is critical for security. We’ll restrict source into a VPC later. Just click “Review and Launch”.

![](@site/static/img/152640820-c9240fa7-9a40-40ed-9d6b-4c0ed3a0b2e1.png)

* In the coming window it will ask to specify key file. Choose “Create new key pair” and click “Download Key Pair” button. After key file is downloaded click “Launch Instances”.

![](@site/static/img/152640822-5719e6e4-fab7-49f2-9bf6-3986d51b9f6d.png)

* Right now, your instances should be launching as shown in the image.

![](@site/static/img/152640823-c87ded9e-aec1-4abb-8f3b-e9442679c28a.png)

* Go to EC2 Instances and Click “Connect” button.

![](@site/static/img/152640824-10f87703-9398-412c-ab07-7b773629e283.png)

* It shows a dialog as follow and connect to instance via ssh.

![](@site/static/img/152640825-06b86d38-f024-4ca9-b545-78d38ad87abe.png)

* Right now, you should connect to your instance. To Connect your instance, open a terminal and run a command something like below. Please change {YOUR\_KEY\_FILE} and {INSTANCE\_PUBLIC\_IP} with your own credentials. For our case, they are “ant.pem” and “3.108.40.66”.

  ssh -i {YOUR_KEY_FILE} ubuntu@{INSTANCE_PUBLIC_IP}

### **Install MongoDB to Your Instance*** After you get connected, run the following commands in order to install MongoDB to your instance.

```shell
wget -qO - https://www.mongodb.org/static/pgp/server-5.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/5.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-5.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org
```

* Open /etc/mongod.conf file with an editor like nano and change bind\_ip value to 0.0.0.0 to let MongoDB accept connections in all interfaces and save it.

```sudo nano /etc/mongod.conf```

![](@site/static/img/mongodb.png)

Press “Ctrl + X” to save the file.

Restart mongod and enable service.

sudo systemctl restart mongod
sudo systemctl enable mongod.service
MongoDB installation is complete, just save your MongoDB instance’s local address somewhere. We will use it in later.

### **Step 2: Install Scalable Origin Group**

* Click “Auto Scaling >` Launch Configurations” and Click “Create Launch Configuration”.

![](@site/static/img/152642513-780c642f-a689-4923-a160-dc88ae7a1afb.png)

* You can see the name field just under the Create Launch Configuration header. Give a name something like “OriginGroup”.
* In Launch configuration you need to search AMI of Ant media server using image Id as per your AWS region. You can see the image Ids [here](https://ami.antmedia.io/). For example, we are using ap-south-1 image Id in our cluster as shown in below image.

![](@site/static/img/image-1645168998733.png)

* Choose instance type, in our sample we choose c5.xlarge. You can choose any instance type according to your project and after proceed to next step.

![](@site/static/img/152643648-16a80676-25c5-4fe5-9517-0f46624a4b36.png)

* In the coming window as shown in the image below, We need to give name and set User data.

  * Then Click “Advanced Details” title. You will see the “User data” text area. Right now, copy the text below, change the “`{MongoIP}`” field with the MongoDB IP Address in the script and paste it to the “User data”.
  * After that Click “Skip to review”

  ```
  #!/bin/bash
  cd /usr/local/antmedia
  ./change_server_mode.sh cluster {MongoIP}
  ```

The form should be something like below.

![](@site/static/img/152643848-f5a0cfb9-5682-40fb-957d-3649e8315f11.png)

* Now you have to create new security group for Auto scaling group in which below ports need to be whitelisted as shown in image.

Important Note:

**UDP: 50000-60000 (WebRTC. In v2.4.3 and higher, the default range is 50000-60000. Prior to 2.4.3, the default value was 5000-65000. Note that, you can** [**change the port range**](https://stackoverflow.com/questions/62127593/how-to-limit-the-webrtc-udp-port-range-in-ant-media-server) **in all releases).**

**TCP: 5000 (This port needs to open only in cluster mode for internal network communication).**

![](@site/static/img/Screenshot(44).png)

* Click “Create Launch Configuration”.
* After launch configuration is created successfully, go to Auto Scaling Groups in EC2 section and create Auto Scaling Group.

![](@site/static/img/152644118-b5f72bfe-8c4a-4ef3-b2b0-aafcf67a577d.png)

* Give a name to scaling group. We give “AMS-Origin-Group” as a name and then Switch to launch configuration as by default it is selected to Launch template. Select your launch configuration group that you have created earlier for origin group as shown in below image.

![](@site/static/img/152644315-f2c607a7-a1ec-4b33-82bf-ba1ff05e2d18.png)

* Choose “ap-south-1a” subnet. We choose only one subnet to let all instances appear in the same subnet for having better connectivity.

![](@site/static/img/152644434-9a3edc0e-456f-4eb7-a616-0820038e6aa8.png)

* In Configure advance options you need to select existing load balancer option that we will create later to attach auto scaling groups with it.

![](@site/static/img/152644662-62b0756a-454b-465c-a2e4-f8d0a5e2789c.png)

* Choose your scaling policy. In our sample below, our origin group will scale up to maximum 10 instances by providing Average CPU Utilization with %60. Then Click Next and Next.

![](@site/static/img/152644742-73936897-0ff1-4e39-894a-c10f0319de4b.png)

* Lastly, Review screen will come and click the “Create Auto Scaling group”.

### **Step 3: Install Scalable Edge Group**

Installing scalable edge group almost same as scalable origin group. Please go to Step 2 again and follow same steps one more time. Just don’t forget to change naming (for instance give group name as Edge Group) and configure scaling policy and instance type according to your needs. If you have any question or problem with this, please let us know through [support@antmedia.io](mailto:support@antmedia.io).

### **Step 4: Install Load Balancer**

* Click the “Load Balancing >` Load Balancers” on EC2 Service and Click the “Create” button under Application Load Balancer.

![](@site/static/img/152646071-b22ea083-c9d9-43ae-a98c-424f8a566a51.png)

* Give a name to your Load Balancer and basic configuration should be like in image below and choose ap-south-1a and ap-south-1b for availability zones.

![](@site/static/img/152648356-a6f8a563-5edd-4242-8e83-4686e8aa6f43.png)

* Now we need to choose load balancer security group which we will create by clicking create new security group option.

![](@site/static/img/152646691-d76e78b8-7009-4130-8b2f-450062a55998.png)

* Before moving further in Load balancer configuration we need to create target groups for both Origin & Edge and forward with HTTP through 5080 port. In the Register Targets group, do nothing, just proceed because we bind target later. In below example we have created origin target group and same for Edge will be created.

![](@site/static/img/152647166-ae859419-7bc9-49ab-871f-392fc5fc7cf1.png)

* After creating Target Groups, again go to EC2 >` Target Groups >` Edit attributes and change the Load Balancing algorithm for Edge and Origin as below.

![](@site/static/img/152652226-86f30378-977e-4b53-8192-21f9c27d8b50.png)

* Now continue load balancer configuration, choose both HTTP and HTTPS by clicking “Add listener”. The port settings should be like in the image below and we need to bind target groups now to forward requests to origin & edge target groups. Also in Auto scaling groups advance configuration please attach the created load balancer now.

![](@site/static/img/152647506-8eb95d60-e12b-4a1a-b9cc-3d5fd00c6fe0.png)

* For the next versions, you need to configure as follows. After adding these rules, you can reach edge/origin using a single url for example [https://yourdomain.com/WebRTCAppEE/index.html?target=origin](https://yourdomain.com/WebRTCAppEE/index.html?target=origin) to the origin cluster and [https://yourdomain.com/WebRTCAppEE/index.html?target=edge](https://yourdomain.com/WebRTCAppEE/index.html?target=edge) to the edge. You will be able to reach. In other words, we are eliminating the 5443, 443 port separation.

Click ```Load Balancer >` Your LoadBalancer >` HTTPS: 443 >` View/Edit Rules``` and add 2 rules as below.

![](@site/static/img/aws-rules.png)

* Now Choose your domain certificate in Secure listener settings for secure streaming (If you don’t know how to create certificate for ACM, [please follow this guide](https://antmedia.io/ssl-from-aws-certificate-manager-for-domain-name/) and create load balancer. Also don’t forget to add CNAME for your load balancer. For instance, every load balancer has a DNS name like “xxxx.ap-south-1.elb.amazonaws.com” so that you need to add CNAME for your subdomain that points to your load balancer address.

Right now Everything is ok. Just let me give a brief information about the difference between publish and play. In our load balancer configuration, we forward HTTP(80) and HTTPS(443) to Origin Group and we forward HTTP(5080) and HTTPS(5443) to Edge Group. It means that we should connect 80 or 443 ports to publish and connect 5080 or 5443 to play streams. Otherwise, play requests goes to origin group and publish request goes to edge group and it’s likely create some performance issues according to your configurations.

> Quick Link: [How to configure RTMP Load Balancer in AWS ?](/v1/docs/how-to-configure-rtmp-load-balancer-in-aws)

### **Logging in Web Panel**

You can login to web panel via the [https://your-domain-name/](https://your-domain-name/) and login with “JamesBond” and the first instances instance-id in your origin group. If you don’t know the instance-id, you need to change your password.

We are storing passwords with MD5 encryption in the latest version. You can encrypt your password basically as follows.

On the terminal program

echo -n 'new-password' | md5sum
or any MD5 encrypter page like: [https://www.md5online.org/md5-encrypt.html](https://www.md5online.org/md5-encrypt.html)

Please ssh to your MongoDB instance and write the below commands via terminal

$ mongo

> `use serverdb` db.getCollection('User').find()
> ` db.User.updateOne({"_id": "5e978ef3c9e77c0001228040"}, {$set:{password: "md5Password"}})
>  It gives you an output like this

```{ "_id" : ObjectId("5e978ef3c9e77c0001228040"), "className" : "io.antmedia.rest.model.User", "email" : "JamesBond", "password" : "e4e6ca42342f95978a17c6257593c1e1", "userType" : "ADMIN" }```

### Enable IP Filtering

Please visit [How to enable IP filter behind a load balancer?](/v1/docs/how-to-enable-ip-filter-for-ant-media-servers-behind-load-balancer-in-aws)

### Test Flight

For publishing please visit the ```https://your-domain-name/WebRTCAppEE/``` and click “Start Publishing” button. The default stream id is “stream1” For playing please visit the ```https://your-domain-name:5443/WebRTCAppEE/player.html``` and click “Start Playing” button. The default stream will be played.

As you figure out, we connect default https port(443) for publishing and 5443 port for playing. Because we configure load balancer to forward default port(443) to origin group and 5443 to edge group.
