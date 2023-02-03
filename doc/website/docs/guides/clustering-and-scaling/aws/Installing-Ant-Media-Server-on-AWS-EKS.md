# Installing Ant Media Server on AWS EKS

In this post, we are going to guide you on how to run Ant Media Server on AWS EKS step by step.

1\. After you are logged in to AWS, search the **EKS** keyword, find the **Elastic Kubernetes Service,** and click the **Add Cluster >` Create** button.

![image.png](@site/static/img/image-286329.png)

* * *

2\. After setting a name for your cluster, the Kubernetes version and Cluster Service Role should be selected. You can follow this link to create a [Cluster Service Role](https://docs.aws.amazon.com/eks/latest/userguide/service_IAM_role.html).

![image.png](@site/static/img/image-286429.png)

* * *

3\. In this section, subnets under VPC and VPC should be selected and a security group should be created.

![image.png](@site/static/img/image-286529.png)

Endpoint access should be selected as **Public** and the **Next** button is clicked.

![image.png](@site/static/img/image-286629.png)

* * *

4\. You can activate the following options for logging.

![image.png](@site/static/img/image-286729.png)

* * *

5\. Let’s check the configurations you set and create the cluster by clicking the **Create** button.

![image.png](@site/static/img/image-286829.png)

* * *

6\. When your cluster’s status is changed from pending to active, click on the **Configuration >` Compute** tab and click on the **Add Node Group** button.

![image.png](@site/static/img/image-286929.png)

* * *

7\. Type your node name and create the [Node IAM Role](https://docs.aws.amazon.com/eks/latest/userguide/create-node-role.html).

![image.png](@site/static/img/image-287029.png)

* * *

8\. Click on the **Next** button after you configure the scaling of the **AMI type, Capacity type, Instance type, Disk and Node Group**.

![image.png](@site/static/img/image-287129.png)

* * *

9\. Select your subnets and click on the **Next** button.

![image.png](@site/static/img/image-287229.png)

* * *

10\. Finally, after checking the configurations, create the Node Pool by clicking on the **Create** button.

![image.png](@site/static/img/image-287329.png)

* * *

11\. Update your Kubernetes **kubeconfig** settings as below, then list your nodes with the **kubectl get nodes** command.

    aws eks --region your_region update-kubeconfig --name clustername
    

![image.png](@site/static/img/image-287429.png)

* * *

12\. Now, it’s time to deploy the Ant Media Server. Create the **yaml** files in order as follows.

First, you should organize your image field since you are going to change images. Here are the steps to organize your image field:

    wget https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-deployment.yaml 
    kubectl create -f ams-k8s-deployment.yaml
    

    kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-hpa.yaml
    kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-rtmp.yaml 
    wget https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-ingress.yaml
    

[Deploy the ingress.](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Ingress)

Once the changes on the **ams-k8s-ingress.yaml** file are done, let’s create our ingress.

    kubectl create -f ams-k8s-ingress.yaml
    

If everything works well, you will see the public IP address/domain name in the **kubectl get ingress** command’s output. After you make your DNS registration, you will be able to access over the domain you have determined.

![image.png](@site/static/img/image-287529.png)

Run **kubectl get services** command to get the RTMP address. You can send broadcasts over 1935 to the domain name that appears as EXTERNAL-IP.

![image.png](@site/static/img/image-287629.png)

When we check the AMS dashboard, we can see that 2 nodes have joined the cluster.

![image.png](@site/static/img/image-287729.png)
