# Configuring RTMP LB in AWS

Follow the instructions below to configure RTMP Load Balancer in Ant Media Server Auto Scaling structure.

**1.** Click on Create Load Balancer and create a new Load Balancer. (EC2 >` Load Balancers >` Create Load Balancer )

![](@site/static/img/aws-rtmp-2.png)

**2.** Create a Classic Load Balancer.

![](@site/static/img/aws-rtmp-3.png)

**3.** Adjust the settings as in the red rectangles.

![](@site/static/img/aws-rtmp-4.png)

**4.** In this section, select Create a New Security Group and adjust the settings as in the Red rectangle.

![](@site/static/img/aws-rtmp-5.png)

**5.** Proceed by clicking “Next: Configure Health Check” button.

![](@site/static/img/aws-rtmp-6.png)

**6.** Proceed by clicking “Next: Add EC2 Instances” button.

![](@site/static/img/aws-rtmp-7.png)

**7.** Press the Next button without doing anything here.

![](@site/static/img/aws-rtmp-8.png)

**8.** Click the Review and Create button here.

![](@site/static/img/aws-rtmp-9.png)

**9.** Please proceed by clicking "Create"

![](@site/static/img/aws-rtmp-10.png)

**10.** Finish the Load Balancer setup.

![](@site/static/img/aws-rtmp-11.png)

**11.** Auto Scaling >` Go to Auto Scaling >` Auto Scaling Groups and select your Origin Group.

![](@site/static/img/aws-rtmp-13.png)

**12.** Edit the Load Balancing setting from this section.

![](@site/static/img/aws-rtmp-14.png)

**13.** In this window, add the Classic Load Balancer you have added to the Choose A Load Balancer section and click the Update button.

![](@site/static/img/aws-rtmp-15.png)

**14.** If everything is fine, you should see the origin instance when you click the Instance tab from Load Balancing>` Load Balancers>` Antmedia-RTMP-LB.

![](@site/static/img/aws-rtmp-16-1.png)

Since a new load balancer has been installed, your RTMP URL will be the Load Balancer URL.


**Example:**

rtmp://Antmedia-RTMP-LB-962025612.eu-west-2.elb.amazonaws.com/WebRTCAppEE/
![](@site/static/img/aws-rtmp-url.png)
