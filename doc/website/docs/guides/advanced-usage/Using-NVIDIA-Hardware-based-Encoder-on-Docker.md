# Using NVIDIA Hardware-based Encoder on Docker

You can use NVIDIA hardware-based encoder on Docker with Ant Media Server.

### Requirements

On host(18.04 and 20.04)

*   [Install CUDA Drivers](https://resources.antmedia.io/docs/using-nvidia-gpus)
*   Install docker-ce according to the link - [https://docs.docker.com/install/](https://docs.docker.com/install/)

    sudo apt-get install apt-transport-https ca-certificates curl gnupg-agent software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    sudo apt-get update
    sudo apt-get install docker-ce docker-ce-cli containerd.io

**1**. Add Repos for nvidia-docker2

    curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
    distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
    curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | sudo tee /etc/apt/sources.list.d/nvidia-docker.list
    sudo apt-get update

**2.** Install nvidia-docker2 for Ubuntu 18.04 and 20.04

    sudo apt-get install -y nvidia-docker2
    sudo pkill -SIGHUP dockerd

**3.** Start a docker container with following command

*   Ubuntu 18.04

    sudo docker run --runtime=nvidia \
     --privileged --network host --name cuda-docker2 \
     -e NVIDIA_VISIBLE_DEVICES=all -e NVIDIA_DRIVER_CAPABILITIES=compute,utility,video \
     -it nvidia/cuda:10.0-runtime-ubuntu18.04

*   Ubuntu 20.04

    sudo docker run --runtime=nvidia \
     --privileged --network host --name cuda-docker2 \
     -e NVIDIA_VISIBLE_DEVICES=all -e NVIDIA_DRIVER_CAPABILITIES=compute,utility,video \
     -it nvidia/cuda:11.7.0-runtime-ubuntu20.04

4\. In this docker container, you can install Ant-Media-Server Enterprise edition. It automatically uses hardware encoder. Alternatively, you can use [Ant Media Server Docker file](https://github.com/ant-media/Scripts/blob/master/docker/Dockerfile_Process) and just change the line **FROM ubuntu:20.04** to \`**FROM nvidia/cuda:11.7.0-runtime-ubuntu20.04**'. After that build the image and run the container with below command.

    sudo docker run -d --name antmedia --runtime=nvidia --privileged --network host  \
    -e NVIDIA_VISIBLE_DEVICES=all -e NVIDIA_DRIVER_CAPABILITIES=compute,utility,video \
    -it antmediaserver