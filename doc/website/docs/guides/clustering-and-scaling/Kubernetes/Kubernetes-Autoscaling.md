# Kubernetes Autoscaling

Kubernetes lets you scale the pods automatically to optimize resource usage and make the backend ready according to the load in your service. Horizontal Pod Autoscaler which is a built-in component can scale your pods automatically.

Firstly, we need to have a Metrics Server to collect the metrics of the pods. To provide metric via the Metrics API, a metric server monitoring must be deployed on the cluster. Horizontal Pod Autoscaler uses this API to collect metrics.

Install Metric Server
=====================

Metric Server is usually deployed by the cloud providers. If you are using a custom Kubernetes or the Metric Server is not deployed by your cloud provider you should deploy it manually as explained below. Firstly, Check if metrics-server is installed using the command below.

    kubectl get pods --all-namespaces | grep -i "metric"

You are going to see an output exactly like the below.

    kube-system   metrics-server-5bb577dbd8-7f58c           1/1     Running   7          23h
    

  

###     [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Autoscaling#manual-installation)Manual Installation

Download the components.yaml file on the master.

  

    wget https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

Add the following line to line 132 of the file. ```--kubelet-insecure-tls```. The lines are going to seem exactly as below.

    spec:
         containers:
         - args:
           - --kubelet-insecure-tls
           - --cert-dir=/tmp
           - --secure-port=4443
           - --kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname
           - --kubelet-use-node-status-port
           image: k8s.gcr.io/metrics-server/metrics-server:v0.4.2
    

  

Deploy the YAML file that we have made changes.

    kubectl apply -f components.yaml
    

  

Check whether everything is working properly.

    kubectl get apiservices |grep "v1beta1.metrics.k8s.io"

  

The output of the command should be as follows.

    v1beta1.metrics.k8s.io                 kube-system/metrics-server   True        21h

  

    [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Autoscaling#create-horizontal-pod-autoscaling)Create horizontal pod autoscaling
====================================================================================================================================================

First, make a small change in our yaml file in Ant Media Server by running```kubectl edit deployment ant-media-server```. Edit and save the following lines under the container according to yourself. Before proceeding let us tell you about Millicores. Millicores is a metric which is used to measure CPU usage. It is a CPU core divided into 1000 units (milli = 1000). 1000 = 1 core. So the below configuration uses 4 cores.

    resources:
      requests:
      cpu: 4000m
    

  

After adding file content should be like as follows:

    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: ant-media-server
    spec:
      selector:
        matchLabels:
          run: ant-media-server
      replicas: 1
      template:
        metadata:
          labels:
            run: ant-media-server
        spec:
          hostNetwork: true
          containers:
          - name: ant-media-server
            imagePullPolicy: IfNotPresent  # change this value accordingly. It can be Never, Always or IfNotPresent
            image: ant-media-server-enterprise-k8s:test  #change this value according to your image.
            args: ["-g", "true", "-s", "true", "-r", "true", "-m", "cluster", "-h", "mongo"]
            resources:
              requests:
                cpu: 4000m
    

Check the accuracy of the value we entered using the command below.

    kubectl describe deployment/ant-media-server

Now that the deployment is running, we're going to create a Horizontal Pod Autoscaler for it:

    kubectl autoscale deployment ant-media-server --cpu-percent=60 --min=1 --max=10

or you can use the following yaml file:

    kubectl create -f https://raw.githubusercontent.com/ant-media/Scripts/master/kubernetes/ams-k8s-hpa.yaml

In the above configuration, we set the CPU average as 60% and we set the pods as min 1 and maximum 10. A new pod will be created every time the CPU average passes 60%.

You can monitor the situation in the following output.

    root@k8s-master:~# kubectl get hpa
    NAME               REFERENCE                     TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
    ant-media-server   Deployment/ant-media-server   3%/60%   1         10         1          20h

New pods are going to be created when we start loading and the cpu exceeds 60%. When the cpu average value decreases below 60%, then the pods are going to be terminated.

    root@k8s-master:~# kubectl get hpa
    NAME               REFERENCE                     TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
    ant-media-server   Deployment/ant-media-server   3%/60%   1         10         1          20h

Check the number of pods running using the following command.

    root@k8s-master:~# kubectl get pods
    NAME                                READY   STATUS    RESTARTS   AGE
    ant-media-server-7b9c6844b9-4dtwj   1/1     Running   0          42m
    ant-media-server-7b9c6844b9-7b8hp   1/1     Running   0          19h
    ant-media-server-7b9c6844b9-9rrwf   1/1     Running   0          18m
    ant-media-server-7b9c6844b9-tdxhl   1/1     Running   0          47m
    mongodb-9b99f5c-x8j5x               1/1     Running   0          20h

  

    [](https://github.com/ant-media/Ant-Media-Server/wiki/Kubernetes-Autoscaling#utilities)Utilities
----------------------------------------------------------------------------------------------------

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