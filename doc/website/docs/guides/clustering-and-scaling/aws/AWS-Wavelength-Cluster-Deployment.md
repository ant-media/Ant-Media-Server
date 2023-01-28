# AWS Wavelength Cluster Deployment

In this documentation, we would like to demonstrate how to set up an Auto-Scalable Ant Media Server Cluster on the Wavelength zone. We will be using the Cloudformation template and we will go over the steps to set up a cluster.

Before we start with the installation, let’s take a look at what exactly the template does. The template generates the VPC, Carrier IP and Internet Gateway configuration automatically. The VPC network it uses has the ```10.0.0.0/16, 10.0.10.0/24, 10.0.11.0/24``` subnets in use. It also configures 2 NGINX Instances for load balancing and these operate as Active/Passive. These instances are also assigned the addresses ```10.0.10.201``` and ```10.0.11.201```.

Prerequisite
------------

*   Subscribe the [Ant Media Server on AWS Marketplace](https://aws.amazon.com/marketplace/pp/prodview-464ritgzkzod6). If you don't know how to do that [just follow the 3 steps here](/v1/docs/cloudformation-installation-for-scaling-ant-media-server)

Step by Step Deployment
-----------------------

### 1\. Create an Elastic IP for Your Load Balancer

As Cloudformation doesn’t support Elastic IP in the Wavelength Zone yet, let’s create an Elastic IP as shown below. You should also create a DNS record for this elastic IP address.

Click on ```EC2 >` Elastic IPs``` under Services and then click on ```Allocate Elastic IP address```.  
![](@site/static/img/wavelength-eip1.png)

### 2\. Choose Your Wavelength Zone

On the next page, pick the correct Wavelength Zone as the Network Border Group. The Wavelength Zones include WLZ.  
![](@site/static/img/wavelength-eip2.png)

### 3\. Allocate the Elastic Carrier IP

Click on allocate to allocate your new Elastic Carrier IP address. **Please note the Allocation ID of the Elastic Carrier IP before continuing with the Cloudformation part of the installation.****![](@site/static/img/wavelength-eip3.png)**

### 4\. Import the Cloudformation Template

Download the template from the link below and open the Cloudformation service to import the template. Link: **Link here****![](@site/static/img/wavelength-cf1.png)**

### 5\. Fill the Form

Here are field explanations in the form

*   **Stack Name:** it will describe your stacks, it will be like a skeleton
*   **AllocationID:** Allocation ID of ElasticIP
*   **AntMediaEdgeCapacity:** How many Edge servers will be created.
*   **AntMediaEdgeCapacityMax:** Edge Server where Auto Scale will reach maximum
*   **AntMediaOriginCapacity:** How many Origin servers will be created.
*   **AntMediaOriginCapacityMax:** Origin Server where Auto Scale will reach maximum  
    ![](@site/static/img/wavelength-cf2.png)
*   **CPUPolicyTargetValue:** Average CPU utilization of the Auto Scaling group. When the server reaches %40 CPU utilization average, new servers will be added
*   **EdgeInstanceType:** Edge Instance Type
*   **KeyName:** An Amazon EC2 key pair name. If there is no value here, you must create an ssh key (EC2 >` Key Pairs).
*   **MongoDBInstanceType:** MongoDB Instance Type
*   **NginxInstaceType:** Nginx Instance Type
*   **OriginInstanceType:** Origin Server Instance Type
*   **PolicyName:** The policy name that has full access to EC2
*   **StunServerAddress:** Stun Server URL or IP address
*   **WavelengthZones:** Available Wavelength Zones  
    ![](@site/static/img/wavelength-cf3.png)

### 6\. Click "Next"

Please proceed by clicking “Next” button  
![](@site/static/img/wavelength-cf4.png)

### 7\. Review the Deployment

In this section, you can view and check the summary of the parameters you have entered and you can edit it here as below.  
![](@site/static/img/wavelength-cf5-1.png)

### 8\. Allow IAM permissions

We are using AWS Lambda to get the Latest Ant Media Server image from AWS Marketplace, so IAM permissions are needed to get our latest image.  
![](@site/static/img/wavelength-cf5-2.png)

### 9\. Wait the Deployment to be Completed

If the template has been installed successfully, it says "Create Complete" in the red rectangle.  
![](@site/static/img/wavelength-cf6.png)

### 10\. Review the Instances

When the installation is finished, the instances will look like this.  
![](@site/static/img/wavelength-cf7.png)

### 11\. Import SSL Certificate

In this configuration, Nginx uses a self-signed certificate. In order not to get a certificate error, you need to change it to your own certificate or a Let’s Encrypt certificate path in the nginx.conf file.

You need to find the following lines and edit them according to the path of your own certificate.

```vim /etc/nginx/nginx.conf```

    ssl_certificate /etc/nginx/ams.crt;
    ssl_certificate_key /etc/nginx/ams.key;

#### Creating a certificate with Let's Encrypt

You need to generate certificates with the DNS-01 challenge due to the restrictions in the Wavelength zone.

You can create a certificate by following the steps below, and don't forget to generate the certificate on all Nginx instances.

**Option1:** If your domain name is on Route53, you can assign policies to Nginx instances and generate automatic certificates with the following command.

    certbot certonly --dns-route53 --agree-tos --register-unsafely-without-email -d yourdomain.com

 **Option2:** If you are using a DNS provider other than Route53, you can create a certificate with the following command. The thing you need to pay attention to here is that you should add the CNAME records given to you by the command in your DNS provider.

    certbot --agree-tos --register-unsafely-without-email --manual --preferred-challenges dns --manual-public-ip-logging-ok --force-renewal certonly -d yourdomain.com

After your certificate is created, you can edit the nginx.conf file like the one below.

    ssl_certificate /etc/letsencrypt/live/{YOUR_DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/{YOUR_DOMAIN}/privkey.pem;

Then restart the service using the systemctl restart nginx command.