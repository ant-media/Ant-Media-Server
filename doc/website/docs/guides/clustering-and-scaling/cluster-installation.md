---
title: Cluster Installation
sidebar_position: 2
---
# Cluster installation

AMS can run in cluster mode. This way, a number of AMS nodes can work together to increase the number of viewers and publishers. In other words, you can publish a live stream to one node of AMS in the cluster and you can watch the stream in another node in the cluster.

![](@site/static/img/origin_edge.png)

AMS cluster has 4 main components.

1. **Database (MongoDB):** Stream information is recorded to the database to let all nodes access the data. Stream information contains bitrates, settings, origin node of the stream, and other data.
2. **Origin group:** This group consists of AMS nodes that ingest streams and do the necessary actions such as transcoding, transmuxing, etc. Nodes in origin group distribute the streams to the nodes in the edge group. Viewers don't get connected to the nodes in the origin group to play streams. Nodes in the origin group are suggested to have GPU, if adaptive bitrates are enabled in the cluster.
3. **Edge group:** This group consists of AMS nodes that get streams from nodes in the origin group and send to the viewers. Nodes in this group should not ingest stream and these nodes don't not perform any actions like transcoding or transmuxing. They only get the stream from origin and send it to the viewers.
4. **Load balancer:** This component is the frontend for the viewers and publishers. It receives the request from the users and forwards the request to a node in the origin or edge group. It balances the incoming load into the nodes running in the backend.

## Basics of clustering

* Each instance registers itself to the MongoDB database.
* When an instance starts receiving a live stream, it registers itself as the origin of the stream.
* When the load balancer forwards a play request to any of the nodes in the edge group,
  * Node gets the stream origin from MongoDB.
  * Node fetches the stream from the origin node.
  * Node distributes the stream to viewers.

**Important note:** You need to open TCP port range (TCP:5000-65000) to the internal network. It should not be open to public.

To run AMS in a cluster please follow these steps.

## Installing the Mongodb database

You can install MongoDB to an instance or even you can make cluster installation for MongoDB. In this documentation, we explain how to install MongoDB to an Ubuntu Linux machine. As the commands are specific to Ubuntu, you can use corresponding Linux commands to deploy to a yum based Linux distribution as well.

Connect your instance and run the following commands. Make sure you download the latest MongoDB edition by modifying the version number below.

```shell
wget -qO - https://www.mongodb.org/static/pgp/server-5.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/5.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-5.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org
```

After MongoDB 4.4, if the amount of open files ulimit is below 64000, you may encounter a startup error. For this, add the following lines under `/etc/security/limits.conf`

```shell
root soft     nproc          65535  
root hard     nproc          65535   
root soft     nofile         65535   
root hard     nofile         65535

mongodb soft     nproc          65535
mongodb hard     nproc          65535
mongodb soft     nofile         65535
mongodb hard     nofile         65535
```

Set ```bind_ip``` value as ```0.0.0.0``` in ```/etc/mongod.conf``` file to let MongoDB accept connections from other nodes.

0.0.0.0 means ```listen on every available network interface```. If you don't have a firewall, you will accept all connection from everywhere to your MongoDB server. We recommend adding security credentials to your MongoDB instance with following commands.

First see if Mongodb has started:

```shell
sudo systemctl status mongod
```

If not, start Mongodb using the following commmand:

```shell
sudo systemctl start mongod
```

Now connect to MongoDB shell:

```shell
mongo
```

Create an admin user and a password (make sure to change the values for ```user``` and ```pwd``` fields)

use admin

```js
db.createUser(
    {
        user: "superadmin",
        pwd: "admin",
        roles: [ "root" ]
    }
)
```

Enable security in MongoDB configuration.

```shell
sed -i 's/#security:/security:\n  authorization: "enabled"/g' /etc/mongod.conf
```

Restart **mongod** service.

```shell
sudo systemctl restart mongod 
```

* Enable MongoDB start at boot

```shell
sudo systemctl enable mongod.service
```

* Restart MongoDB

```shell
sudo systemctl restart mongod
```

## Install the origin and edge groups

You can easily switch AMS from ```standalone``` mode to ```cluster``` mode or vice versa. Let's switch AMS from  standalone mode to cluster mode.

In order to configure AMS run in cluster mode, you just need to run the below command.

<InfoBox>
If you have set up a username and password for MongoDB, then you need to pass the credentials in the command:
</InfoBox>

**Without credentials**

```shell
cd /usr/local/antmedia
sudo ./change_server_mode.sh cluster <MONGODB_SERVER_IP>
```

**With credentials**

```shell
cd /usr/local/antmedia
sudo ./change_server_mode.sh cluster <MONGODB_SERVER_IP> <MONGODB_USERNAME> <MONGODB_PASSWORD>`
```

For **MongoDB Atlas** connections, you can give the direct ```mongodb+srv``` URL as follows

```shell
sudo ./change_server_mode.sh cluster mongodb+srv://<username>:<password>@<url>/<name>?<params>
```

You can monitor all nodes in the cluster by visiting the web page below in any node.

`http://<ANT_MEDIA_SERVER_NODE_IP>:5080/#/cluster`

## Installing the load balancer

Install the load balancer using either one of the below two options. AMS uses Nginx by default, bu you can also use HAProxy as your load balancer. You can read how to install either of these options in the documents below.

* [Nginx Load Balancer](/v1/docs/installing-with-nginx-load-balancer)
* [HAProxy Load Balancer](/v1/docs/load-balancer-with-haproxy-ssl-termination)
