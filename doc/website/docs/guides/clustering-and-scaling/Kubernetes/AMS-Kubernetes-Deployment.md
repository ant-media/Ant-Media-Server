# AMS Kubernetes Deployment

Sample AMS Deployment File
--------------------------

AMS has such a deployment file structure. This file has a few differences according to the deployment type. Here we will introduce the general file structure.

    kind: Service
    apiVersion: v1
    metadata:
      name: ant-media-server
    spec:
      selector:
        app: ant-media
      ports:
        - name: http
          protocol: TCP
          port: 5080 
    ---
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: ant-media-server
    spec:
      selector:
        matchLabels:
          app: ant-media
      replicas: 1
      template:
        metadata:
          labels:
            app: ant-media
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchExpressions:
                  - key: app
                    operator: In
                    values:
                    - ant-media
                topologyKey: "kubernetes.io/hostname"
          hostNetwork: true
          dnsPolicy: ClusterFirstWithHostNet
          containers:
          - name: ant-media-server
            imagePullPolicy: IfNotPresent # change this value accordingly. It can be Never, Always or IfNotPresent
            image: ant-media-server-enterprise-k8s:test #change this value according to your image.
    # By default, mongodb deployment is used. If you're using mongodb somewhere else, specify it  with server url(-h) below. 
    # You may also need to add -u and -p parameters for
    # specifying mongodb username and passwords respectively         
            args: ["-g", "true", "-s", "true", "-r", "true", "-m", "cluster", "-h", "mongo"]
            resources:
              requests:
                 cpu: 4000m
    

Here are the explanations for the common parameters and the changes parameters.

### **Common Parameters**

  
**The following parameters are common parameters independent of deployment type.**  

*   **imagePullPolicy:** IfNotPresent means that if the image is available in local environment. It'll not be pulled from the private or public registry.
*   **image:** ant-media-server-enterprise-k8s:test specifies the name of the image. You should pay attention here as it should be the same name with the image you built in previous step.
*   **args:**\["-g", "true", "-s", "true", "-r", "true", "-m", "cluster", "-h", "127.0.0.1"\] specifies the parameters for running the Ant Media Server pods. Let us tell their meanings and why we need them.
    *   "**\-g**", "true": It means that Ant Media Server uses the public IP address of the host for internal cluster communication. Default value is false.
    *   "**\-s**", "true": It makes Ant Media Server uses its public IP address as the server name.
    *   "**\-r**", "true": It makes Ant Media Server replaces the local IP address in the ICE candidates with the server name. It's false by default.
    *   "**\-m**", "cluster": It specifies the server mode. It can be cluster or standalone. Its default value is standalone. If you're running Ant Media Server in Kubernetes, it's most likely you're running the Ant Media Server in cluster mode. This means you need to specify your MongoDB host, username, and password as parameter.
    *   "**\-h**", "127.0.0.1": It specifies the MongoDB host address. It's necessary to use if you're running in cluster mode. In this example, it's 127.0.0.1 because in the CI pipeline, local MongoDB is installed. You should change it with your own MongoDB address or replica set.
    *   "**\-u**", "username": It specifies the username to connect to MongoDB. If you don't have credentials, you don't need to specify.
    *   "**\-p**", "password": It specifies the password to connect to MongoDB. If you don't have credentials, you don't need to specify.
    *   "-l", "license number": It makes Ant Media Server uses the license key.

### **Changing Parameters**

**The following parameters are different according to the deployment type.**

*   **hostNetwork:** true line above means that Ant Media Server uses the host network. It is required as there is a wide range of UDP and TCP ports are being used for WebRTC streaming. This also means that you can only use one pod of Ant Media Server in a host instance. Don't worry about where and how to deploy as K8s handles that. We're just letting you know this to determine total number of nodes in your cluster.
*   **affinity: TODO**
*   **labels:** for origin edge distinction TODO

#### Origin & Edge configurations

We strongly recommend separate origin and edge instances in Ant Media Cluster. So we have two sets of deployment files for origins and edges. 

While publishing a stream, you should use the URL of the load balancer of origins. ```ORIGIN_LOAD_BALANCER_URL/WebRTCAppEE```

Similarly, you should use the URL of the load balancer of edges in playing. ```EDGE_LOAD_BALANCER_URL/WebRTCAppEE/player.html```

Kubernetes lets you scale the pods automatically to optimize resource usage and make the backend ready according to the load in your service. Horizontal Pod Autoscaler which is a built-in component can scale your pods automatically.

Firstly, we need to have a Metrics Server to collect the metrics of the pods. To provide metrics via the Metrics API, a metric server monitoring must be deployed on the cluster. Horizontal Pod Autoscaler uses this API to collect metrics.

Create horizontal pod autoscaling
=================================

First, make a small change in our yaml file in Ant Media Server by running```kubectl edit deployment ant-media-server-origin and kubectl edit deployment ant-media-server-edge``` . Edit and save the following lines under the container according to yourself. Before proceeding let us tell you about Millicores. Millicores is a metric which is used to measure CPU usage. It is a CPU core divided into 1000 units (milli = 1000). 1000 = 1 core. So the below configuration uses 4 cores.

    resources:
      requests:
      cpu: 4000m

  

After adding the file content should be like as follows:

    kind: Service
    apiVersion: v1
    metadata:
      name: ant-media-server
    spec:
      selector:
        app: ant-media
      ports:
        - name: http
          protocol: TCP
          port: 5080 
    ---
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: ant-media-server
    spec:
      selector:
        matchLabels:
          app: ant-media
      replicas: 1
      template:
        metadata:
          labels:
            app: ant-media
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchExpressions:
                  - key: app
                    operator: In
                    values:
                    - ant-media
                topologyKey: "kubernetes.io/hostname"
          hostNetwork: true
          dnsPolicy: ClusterFirstWithHostNet
          containers:
          - name: ant-media-server
            imagePullPolicy: IfNotPresent # change this value accordingly. It can be Never, Always or IfNotPresent
            image: ant-media-server-enterprise-k8s:test #change this value according to your image.
    # By default, mongodb deployment is used. If you're using mongodb somewhere else, specify it  with server url(-h) below. 
    # You may also need to add -u and -p parameters for
    # specifying mongodb username and passwords respectively         
            args: ["-g", "true", "-s", "true", "-r", "true", "-m", "cluster", "-h", "mongo"]
            resources:
              requests:
                 cpu: 4000m

Check the accuracy of the value we entered using the command below.  
  

    kubectl describe deployment/ant-media-server-origin
    
    kubectl describe deployment/ant-media-server-edge

Now that the deployment is running, we're going to create a Horizontal Pod Autoscaler for it:  
  

    kubectl autoscale deployment ant-media-server-origin --cpu-percent=60 --min=1 --max=10
    
    kubectl autoscale deployment ant-media-server-edge --cpu-percent=60 --min=1 --max=10

or you can use the following YAML file:  
  

    kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-hpa-origin.yaml
    
    kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-hpa-edge.yaml

In the above configuration, we set the CPU average as 60% and we set the pods as min 1 and maximum 10. A new pod will be created every time the CPU average passes 60%.

You can monitor the situation in the following output.  
  

    root@k8s-master:~# kubectl get hpa
    NAME               REFERENCE                     TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
    ant-media-server   Deployment/ant-media-server   3%/60%   1         10         1          20h

New pods are going to be created when we start loading and the cpu exceeds 60%. When the cpu average value decreases below 60%, then the pods are going to be terminated.  
  

    root@k8s-master:~# kubectl get hpa
    NAME               REFERENCE                     TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
    ant-media-server   Deployment/ant-media-server   52%/60%   1         10         4          20h

Check the number of pods running using the following command.  
  

    root@k8s-master:~# kubectl get pods
    NAME                                READY   STATUS    RESTARTS   AGE
    ant-media-server-7b9c6844b9-4dtwj   1/1     Running   0          42m
    ant-media-server-7b9c6844b9-7b8hp   1/1     Running   0          19h
    ant-media-server-7b9c6844b9-9rrwf   1/1     Running   0          18m
    ant-media-server-7b9c6844b9-tdxhl   1/1     Running   0          47m
    mongodb-9b99f5c-x8j5x               1/1     Running   0          20h

  

  [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Autoscaling#utilities)Utilities
--------------------------------------------------------------------------------------------------

The following command gives information about AutoScale:  
  

    kubectl get hpa

  

Check the load of pods running using the command below:  
  

    kubectl top nodes

  

This command prints out the following:

    root@k8s-master:~# kubectl top node
    NAME         CPU(cores)   CPU%   MEMORY(bytes)   MEMORY%   
    k8s-node     111m         5%     717Mi           38%       
    k8s-node-2   114m         5%     1265Mi          68%       
    k8s-node-3   98m          4%     663Mi           35%       
    k8s-node-4   102m         5%     666Mi           35%       
    n8s-master   236m         11%    1091Mi          58%

### Kubernetes Ingress

We are going to use Nginx as Ingress and install it via Helm.

### Install HELM

Run the following commands to install helm.  
  

    wget -qO- https://get.helm.sh/helm-v3.5.2-linux-amd64.tar.gz | tar zxvf - 
    cd linux-amd64/
    ./helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
    ./helm repo update
    ./helm install ingress-nginx ingress-nginx/ingress-nginx

Or you can install it via the APT tool.  
  

    curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | sudo tee /usr/share/keyrings/helm.gpg >` /dev/null
    sudo apt-get install apt-transport-https --yes
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
    sudo apt-get update
    sudo apt-get install helm

Make sure everything is working correctly with the following command.

```kubectl get pods -n default | grep "ingress"```

###   

  [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Ingress#to-install-an-ssl-certificate)To deploy Ant Media Server with hostNetwork
----------------------------------------------------------------------------------------------------------------------------------------------------

Run the following commands with hostNetwork

*   ```kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-mongodb.yaml```
*   kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-deployment-edge.yaml](https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-deployment-edge.yaml)  
    kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-deployment-origin.yaml](https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-deployment-edge.yaml)
*   ```kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-hpa-origin.yaml```  
    ```kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-hpa-edge.yaml```
*   ```kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-ingress-origin.yaml](https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-ingress-origin.yaml)```  
    kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-ingress-edge.yaml
*   ```kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-rtmp.yaml](https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-rtmp.yaml)```

 [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Ingress#to-install-an-ssl-certificate)To deploy Ant Media Server without hostNetwork
------------------------------------------------------------------------------------------------------------------------------------------------------

Run the following commands without hostNetwork

*   ```kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-mongodb.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-mongodb.yaml)```
*   kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-coturn.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-coturn.yaml)
*   kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-deployment-edge.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-deployment-edge.yaml)  
    kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-deployment-origin.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-deployment-origin.yaml)
*   [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-hpa-origin.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-hpa-origin.yaml)  
    [kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-hpa-edge.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-hpa-edge.yaml)
*   ```kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-ingress-edge.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-ingress-edge.yaml)```   
    ```kubectl create -f [https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-ingress-origin.yaml](https://raw.githubusercontent.com/ant-media/Scripts/babf478b99c7e6b15edbd5aa220fde5ba4cd3adb/kubernetes/ams-with-turn-server/ams-k8s-ingress-origin.yaml)```
*   ```kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-rtmp.yaml```

  [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Ingress#to-install-an-ssl-certificate)To install an SSL certificate
--------------------------------------------------------------------------------------------------------------------------------------

If you have your own certificate, you can add it as follows. If you are going to use Let's Encrypt, you can proceed to the next step.

1.  ```kubectl create secret tls ${CERT_NAME} --key ${KEY_FILE} --cert ${CERT_FILE}```
2.  ```kubectl create secret tls antmedia-cert --key="ams.key" --cert="ams.crt"```

If everything is fine, the output of **kubectl get ingress** will be as follows. So the ADRESS column must have appeared a Public IP address.

    root@kubectl:~# kubectl get ingress
    NAME               CLASS    HOSTS               ADDRESS       PORTS     AGE
    ant-media-server   `<none>`   test.antmedia.io   146.59.2.42   80, 443   94m

### Kubernetes Let's Encrypt Configuration

**Let's Encrypt Configuration**

For this, install Helm and Cert-Manager by following the steps below.

**1.** Begin by adding the Jetstack repository to your Helm installation then update the repo.

    helm repo add jetstack https://charts.jetstack.io
    helm repo update

**2.** Install in your Cert-Manager cluster by running the following line  
  

    helm install cert-manager jetstack/cert-manager --namespace cert-manager --create-namespace --version v1.9.1 --set installCRDs=true

**3.** Install the CustomResourceDefinition resources by using the following command.  
  

    kubectl apply -f https://github.com/jetstack/cert-manager/releases/download/v1.9.1/cert-manager.crds.yaml

**4.** Create a YAML file in your working directory and name it **ams-k8s-issuer-production.yaml** Add the following content:

    apiVersion: cert-manager.io/v1
    kind: ClusterIssuer
    metadata:
      name: letsencrypt-production
    spec:
      acme:
        server: https://acme-v02.api.letsencrypt.org/directory
        email: change_me
        privateKeySecretRef:
          name: letsencrypt-production
        solvers:
          - http01:
              ingress:
                class: nginx

Or you can [download](https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-issuer-production.yaml) it from the GitHub repository.

**Note:** Provide a valid email address. You will receive email notifications on certificate renewals or alerts.  
  

    kubectl create -f ams-production-issuer.yaml

When you run the **kubectl get clusterissuers** command, you will see an output like the one below.

    NAME                     READY   AGE
    letsencrypt-production   True    27m

**5.** You must add an annotation "**cert-manager.io/cluster-issuer: letsencrypt-production**" in the ingress configuration with the issuer or cluster issuer name.  
**YAML file for Origin**

    apiVersion: networking.k8s.io/v1
    kind: Ingress
    metadata:
      name: ant-media-server-origin
      annotations:
        kubernetes.io/ingress.class: nginx
        cert-manager.io/cluster-issuer: letsencrypt-production
        nginx.ingress.kubernetes.io/affinity: "cookie"
        nginx.ingress.kubernetes.io/session-cookie-name: "route"
        nginx.ingress.kubernetes.io/session-cookie-expires: "172800"
        nginx.ingress.kubernetes.io/session-cookie-max-age: "172800"
    spec:
      rules:
      - host: origin.antmedia.cloud
        http:
          paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ant-media-server-origin
                port:
                  number: 5080
    
      tls:
        - hosts:
          - origin.antmedia.cloud
          secretName: ams-certificate-origin

YAML file for Edge

    apiVersion: networking.k8s.io/v1
    kind: Ingress
    metadata:
      name: ant-media-server-edge
      annotations:
        kubernetes.io/ingress.class: nginx
        cert-manager.io/cluster-issuer: letsencrypt-production
        nginx.ingress.kubernetes.io/affinity: "cookie"
        nginx.ingress.kubernetes.io/session-cookie-name: "route"
        nginx.ingress.kubernetes.io/session-cookie-expires: "172800"
        nginx.ingress.kubernetes.io/session-cookie-max-age: "172800"
    spec:
      rules:
      - host: edge.antmedia.cloud
        http:
          paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ant-media-server-edge
                port:
                  number: 5080
    
      tls:
        - hosts:
          - edge.antmedia.cloud
          secretName: ams-certificate-edge

  
**6**. After creating Ingress you should have tls secret in **kubectl get secret** output.

    NAME                                  TYPE                                  DATA   AGE
    ams-certificate-origin                kubernetes.io/tls                     2      44m
    ams-certificate-edge                  kubernetes.io/tls                     2      44m
    default-token-72fnb                   kubernetes.io/service-account-token   3      78m
    ingress-nginx-admission               Opaque                                3      60m
    ingress-nginx-token-ncck2             kubernetes.io/service-account-token   3      60m
    sh.helm.release.v1.ingress-nginx.v1   helm.sh/release.v1                    1      60m
    

**7.** Get the Load Balancer IP address with the **kubectl get ingress** command and add it to your DNS server.

    NAME                      CLASS    HOSTS                   ADDRESS         PORTS     AGE
    ant-media-server-origin   `<none>`   origin.antmedia.cloud   xxx.xxx.xxx.xxx   80, 443   26m
    ant-media-server-edge   `<none>`   edge.antmedia.cloud   xxx.xxx.xxx.xxx   80, 443   26m

**8.** Check whether the certificate has been created by running the **kubectl get cert** command and if you see it as **True**, your certificate will be uploaded to your cluster in a few minutes. Then you can reach it as https://edge.yourdomain.com and [https://origin.domain.com](https://origin.domain.com)