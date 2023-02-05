---
title: Upgrading Ant Media Server
---
This guide will explain how to upgrade Ant Media Server from an earlier version to a more recent version.

-   If you have purchased a license from Ant Media, then in the downloads section of your [antmedia.io](https://antmedia.io/my-account/downloads/) account, you can download the most recent version zip file. 
-   In the case of Ant Media Server's Marketplace image on AWS, Azure, and Alibaba, you can send an email to support@antmedia.io and ask for the new version zip file.
-   In case of Ant Media Server Community Edition, you can download the latest version zip file from [h](https://github.com/ant-media/Ant-Media-Server/releases)[ere](https://github.com/ant-media/Ant-Media-Server/releases).

After downloading the zip file, kindly follow the below steps:

**1. Download the installation script**

Download the`install_ant-media-server.sh` shell script with the latest changes.

Shell

```shell
sudo wget https://raw.githubusercontent.com/ant-media/Scripts/master/install_ant-media-server.sh && sudo chmod 755 install_ant-media-server.sh
```

  
**2. Run the installation script to upgrade the server**

If you want to keep the settings from the previous installation, you must add the **"-r true"** flag at the end of the command.

Shell

```shell
sudo ./install_ant-media-server.sh -i <ANT_MEDIA_SERVER_ZIP_FILE> -r true
```

For change/release logs of new version, please check [here](https://github.com/ant-media/Ant-Media-Server/releases).

### **How do I restore Ant Media Server if needed?**

Last but not least, when you make a fresh installation or upgrade over an older version, the previous installation will be backed up in the **/usr/local** directory with a timestamp value like antmedia-backup-2022-11-18_15-42-54. Now, in order to restore the previous installation, kindly follow the below commands:

Shell

```shell
sudo systemctl stop antmedia
sudo rm -rf /usr/local/antmedia
sudo cp -p -R /usr/local/antmedia-backup_folder/ /usr/local/antmedia
sudo chown -R antmedia:antmedia /usr/local/antmedia/
sudo systemctl start antmedia
```