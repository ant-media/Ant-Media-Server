# Decreasing the boot time

In this post, we are going to guide you to increase your system performance.

To reduce the system boot time, you are may follow the 3 simple steps below.

**1.** You should analyze well which services you will disable here, otherwise, your system may be negatively affected. First, let's check how long your system has booted with the systemd-analyze command. This command shows you how long each service will open.

    root@antmedia:~# systemd-analyze 
    Startup finished in 7.741s (kernel) + 26.704s (userspace) = 34.446s
    graphical.target reached after 7.381s in userspace
    

 Our system was opened in a total of 34.4 seconds. Let's check out why userspace takes so long. You are able to use the following command for this.

    systemd-analyze blame

    11.041s apt-daily.service
              6.842s apt-daily-upgrade.service
              2.484s cloud-init-local.service
              1.610s cloud-init.service
              1.096s systemd-networkd-wait-online.service
              1.027s motd-news.service
               865ms cloud-config.service
               668ms snapd.service
               650ms dev-sda1.device
               580ms fstrim.service
               560ms cloud-final.service
               539ms networkd-dispatcher.service
               511ms lxd-containers.service
               382ms systemd-timesyncd.service
               321ms accounts-daemon.service
               170ms grub-common.service
               161ms keyboard-setup.service
               156ms systemd-modules-load.service
               147ms polkit.service
               136ms systemd-journald.service
               135ms systemd-resolved.service
               129ms snapd.apparmor.service
               121ms apparmor.service
               120ms systemd-udev-trigger.service
               114ms ssh.service

You are able to disable the services you didn't use according to the output above.

For instance, I disable the following services on a server where I don't use cloud structure.

    sudo systemctl disable apt-daily.service
    sudo systemctl disable apt-daily.timer
    sudo touch /etc/cloud/cloud-init.disabled
    sudo systemctl disable motd-news

 The result is below.

    root@antmedia:~# systemd-analyze 
    Startup finished in 7.672s (kernel) + 2.252s (userspace) = 9.925s
    graphical.target reached after 2.242s in userspace

 **2.** By default, your system grub gives you a time of 10 seconds to select between operating systems on a dual boot system.

Edit the following line.

    vim /etc/default/grub

 Change the ```GRUB_TIMEOUT``` line as follows.

    GRUB_TIMEOUT=0

Then run the following command.

    upgrade-grub2

 **3.** If you really need as much performance as possible, you can do one of two things: Use a GUI-less server installation or run the server in run level 3.

    sudo systemctl set-default multi-user.target