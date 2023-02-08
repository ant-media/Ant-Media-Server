# Using Nvidia GPUs

Ant Media Server can use a hardware-based encoder that is available in NVIDIA GPUs. If you have an NVIDIA GPU, you can check whether your GPU contains a hardware-based encoder in [Video Encode and Decode GPU Support Matrix](https://developer.nvidia.com/video-encode-decode-gpu-support-matrix)

**Why use NVIDIA GPU encoder**
------------------------------

The short answer is performance. In some cases, encoding performance increases 5x compared to x264 (CPU) encoder. Note that x264 is one of the best h.264 software encoders, and Ant Media Server uses x264 if there is no GPU in the system.

![](@site/static/img/gpu.png)

Install the CUDA toolkit
------------------------

After you are sure that your GPU contains a hardware based encoder, the only thing left is installing CUDA toolkit to your system.

Installation on Ubuntu 16.04, 18.04 and 20.04
---------------------------------------------

#### Ubuntu 16.04

Get the repo that contains Cuda.

    wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/x86_64/cuda-repo-ubuntu1604_10.2.89-1_amd64.deb

Install repository meta-data.

    sudo dpkg -i cuda-repo-ubuntu1604_10.2.89-1_amd64.deb

Import CUDA Public GPG key.

    sudo apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/x86_64/7fa2af80.pub

#### Ubuntu 18.04

Get the repo that contains Cuda.

    wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64/cuda-repo-ubuntu1804_10.2.89-1_amd64.deb

Install repository meta-data.

    sudo dpkg -i cuda-repo-ubuntu1804_10.2.89-1_amd64.deb

Import CUDA Public GPG key.

    sudo apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/x86_64/7fa2af80.pub

#### Ubuntu 20.04

Run the following commands:

    wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2004/x86_64/cuda-keyring_1.0-1_all.deb 
    dpkg -i cuda-keyring_1.0-1_all.deb

### Continue for Ubuntu 16.04, 18.04, 20.04

Update repository cache.

    sudo apt-get update 

Install Cuda runtime 11.2 ( for v2.3.1+). For version 2.3.0 install 11.0, For versions earlier than 2.2, please install CUDA Runtime 10.0.

    sudo apt-get install cuda-runtime-11-2

If you've installed another version and it does not work, you may want to install compatibility packets.

    sudo apt-get install cuda-cudart-11-2
    sudo apt-get install cuda-compat-11-2

Now you can run the command below to see the status of your GP

    nvidia-smi

You can install Ant Media Server using the usual method, or if you have already installed it, you can restart the Ant Media Server.

    sudo service antmedia restart

Using NVIDIA hardware based encoder
-----------------------------------

Ant Media Server will check and log at startup if there is a hardware-based GPU encoder in the system and it will use it automatically. There is no need to do anything.

If you need more information for installing on other systems, please check [NVIDIA](https://docs.nvidia.com/cuda/cuda-installation-guide-linux/index.html) docs and [CUDA downloads](https://developer.nvidia.com/cuda-downloads?target_os=Linux&target_arch=x86_64&target_distro=Ubuntu&target_version=1604&target_type=debnetwork) pages.