# coTURN quick installation

What is a TURN Server?
----------------------

A TURN server is a network entity in charge of relaying media in VoIP related protocols. This includes SIP, H.323, WebRTC and other protocols.

When you try reaching out directly from one browser to another with voice or video data (sometimes other arbitrary kind of data), you end up going through different network devices. Some of these devices include Firewalls and NATs (Network Address Translators) which may decide due to internal policies not to pass your data.

When there are some network securities like firewall, then data packet does not transfer and we do not get proper streaming of another user.

So we use TURN server for this solution.

Ant Media Server does not require TURN server even if there is Symmetric NAT. However it's required if UDP ports are blocked for any reason or Ant Media Server is used as signaling server in P2P communication.  
  

### Install TURN server

```apt-get update && apt-get install coturn```

### Enable TURN server

Edit the following file.

```vim /etc/default/coturn```

add the below line

```TURNSERVER_ENABLED=1```

### Configure TURN server

Edit the following file.

```vim /etc/turnserver.conf```

just add it to the 2 lines below.

    user=username:password

    realm=your_public_ip_address

and restart TURN server

```systemctl restart coturn```

*   If you use AWS EC2 instance, you need to add extra the below lines

    #EC2 private ip address

    relay-ip=your_private_ip

    #EC2 Public/Private ip address

    external-ip=your_public_ip/your_private_ip

*   Open the following ports on AWS console

    TCP 443 #TLS listening port

    TCP 3478-3479 #coturn listening port

    TCP 32355-65535 #relay ports range

    UDP 3478-3479 #coturn listening port

    UDP 32355-65535 #relay ports range

That 's it.

### How to test Turn Server

#### Command Line

turnutils\_uclient -v -t -T -u username -w password -p 3478 turn\_server\_ip

#### Web Browser

Open the following link and fill in the blanks then ```Add Server```

[https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/](https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/)  
![](@site/static/img/turn1.png)

**Then click ```Gather Candidates```. If everything is fine, you will get the output as in the below image.**

![](https://raw.githubusercontent.com/wiki/ant-media/Ant-Media-Server/images/turn3.png)

### How to add Turn Server to Ant Media sample pages

Go to the codes of index.html, play.html or player.html and change the pc\_config like;

    var pc_config = {
    		'iceServers' : [ {
    			'urls' : 'turn:`<turn_server_address>`:`<port_number>`',
                                 'username': "username",
                                 'credential': "password",
    		} ]
    	};

In v2.4.4 & above, TURN server can be configured on server side. Please check [**here**](https://resources.antmedia.io/docs/configuring-stun-server#configuring-for-ant-media-244-and-later-versions).