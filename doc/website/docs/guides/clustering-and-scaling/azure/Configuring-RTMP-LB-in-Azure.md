# Configuring RTMP LB in Azure

Follow the below instructions to configure RTMP Load Balancer in Ant Media Server Auto Scaling structure.

**1.** Click on Create load balancer and create a new Load Balancer. (Search >` Load Balancing)  
![azure-rtmp-1.png](@site/static/img/azure-rtmp-1.png)

**2.** In this section, select ```Resource Group``` and click ```Next: Frontend IP configuration```.  
![azure-rtmp-2.png](@site/static/img/azure-rtmp-2.png)

**3.** Click ```Add a frontend IP configuration``` in this section and edit as in the screenshot.  
![azure-rtmp-3.png](@site/static/img/azure-rtmp-3.png)

**4.** Now it's time to add the backend pool. Select your Virtual Network and set your Origin scale set.  
![azure-rtmp-4.png](@site/static/img/azure-rtmp-4.png)

![azure-rtmp-5.png](@site/static/img/azure-rtmp-5.png)

**5.** Click ```Add a load balancing rule``` and make your settings as in the screenshot.  
![azure-rtmp-6-1.png](@site/static/img/azure-rtmp-6-1.png)

Now we need to create a Health Probe. Create the health probe as follows.

![azure-rtmp-6-2.png](@site/static/img/azure-rtmp-6-2.png)

**6.** Finally, let's click on ```Review and create``` and create our Load Balancer.

![azure-rtmp-8.png](@site/static/img/azure-rtmp-8.png)

**7.** You can find out the IP address you will use for broadcasting by clicking ```Load Balancing >` Your Load Balancing >` Frontend IP configuration```.

![azure-rtmp-9.png](@site/static/img/azure-rtmp-9.png)