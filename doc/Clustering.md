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

## Dynamic Clustering
Ant Media Server supports dynamic clustering. It means when a new node is added/removed to/from cluster in the same network, all other nodes get notified with UDP multicast messages. It is very easy to handle clustering in dynamic clustering. No need to add or remove any node to anywhere manually.

As mentioned above, Dynamic clustering uses UDP multicast messages so that network where nodes reside support multicasting. If it does not support, static clustering should be used.

## Static Clustering
Ant Media Server supports static clustering. It means new nodes in other networks can be added to cluster.  In addition, if network that nodes reside does not multicasting like AWS VPC, you need to use static clustering.  

In static clustering, all nodes should know what the other nodes' IP addresses are. So all nodes should have the list of all nodes in the cluster. Here is how you can add other nodes statically. 

* Open the `conf/jee-container.xml`file which it is moved from `conf/jee-container-cluster.xml`

* Uncomment the below blocks in `conf/jee-container.xml` . The order of Interceptors are criticial please do not change them.
```xml
<!-- 
<bean class="org.apache.catalina.tribes.group.interceptors.TcpPingInterceptor" />
-->

```
```xml
<!-- 
<bean class="io.antmedia.enterprise.cluster.EasyStaticMemberInterceptor" >
  <constructor-arg>
	  <list>
		  <bean class="org.apache.catalina.tribes.membership.StaticMember" >
			  <property name="port" value="5000" />
				<property name="securePort" value="-1" />
				<property name="hostname" value="10.0.10.227" />
				<property name="domain" value="cluster-antmedia" />
				<property name="uniqueId" value="{10,0,10,227}" />
			</bean>
		</list>
	</constructor-arg>
</bean>	
-->
```

* Add the other nodes in the cluster in the `list` tag as below
```xml
<bean class="io.antmedia.enterprise.cluster.EasyStaticMemberInterceptor" >
  <constructor-arg>
	  <list>
		  <bean class="org.apache.catalina.tribes.membership.StaticMember" >
			  <property name="port" value="5000" />
				<property name="securePort" value="-1" />
				<property name="hostname" value="10.0.10.227" />
				<property name="domain" value="cluster-antmedia" />
				<property name="uniqueId" value="{10,0,10,227}" />
			</bean>
      <bean class="org.apache.catalina.tribes.membership.StaticMember" >
			  <property name="port" value="5000" />
				<property name="securePort" value="-1" />
				<property name="hostname" value="10.0.10.228" />
				<property name="domain" value="cluster-antmedia" />
				<property name="uniqueId" value="{10,0,10,228}" />
			</bean>
		</list>
	</constructor-arg>
</bean>
```

We have added 2 nodes to the xml which have IP addresses 10.0.10.227 and 10.0.10.228. It means there are 3 nodes in the cluster by counting the itself. All nodes should define 2 other nodes in the jee-container.xml file as below.
Please pay attention that uniqueId values are also different and unique. 

In coming versions, we will make this easy to manage. It will be no need to enter these nodes one by one. 


## How Cluster Works

* Clustering in Ant Media Server is masterless(if multicast is enabled in subnet) when a new instance joins the same network, the other instances discover new joined instance and new instance learns all other the instances in the same network.

* When an instance starts to receive live stream, it broadcast a message and other instances know that which instance
is receiving which stream.

* When the load balancer forwards a play request to any of the instances in the network. The instance knows where to fetch 
the live stream from. It fetches live stream and send to audience. 

* Clustering work both for HLS and RTMP live streams




