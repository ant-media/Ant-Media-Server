#!/bin/bash

# -----------------------------------------------------------------------------
# update.sh - Update Script
# -----------------------------------------------------------------------------

# Description:
#   This script facilitates the seamless update of Ant Media Server to the latest version,
#   ensuring you have the most up-to-date features, improvements, and bug fixes. 
#   For more information: https://github.com/ant-media/Ant-Media-Server/releases

# Usage:
#   1. Run the script directly in the terminal using "./update.sh".
#   2. Utilize the dashboard's integrated update feature for a user-friendly experience.

# Note:
#   - Ensure internet connectivity for downloading the latest Ant Media Server release.

# -----------------------------------------------------------------------------

INSTALL_DIRECTORY=/usr/local/antmedia

REMOTE_VERSION=$(curl -s "https://antmedia.io/rest/VERSION" | jq -r .version)
LOCAL_VERSION=$(unzip -p $INSTALL_DIRECTORY/ant-media-server/ant-media-server.jar | grep -a "Implementation-Version"|cut -d' ' -f2 | tr -d '\r')

# If not installed jq package, install it
if [ ! command -v jq &> /dev/null ]; then
    sudo apt-get install -y jq
fi

check_ams() {

	#Download the latest version of the installation script
	wget -O install_ant-media-server.sh https://raw.githubusercontent.com/ant-media/Scripts/master/install_ant-media-server.sh && sudo chmod 755 install_ant-media-server.sh
	get_license_key=`cat $INSTALL_DIRECTORY/conf/red5.properties  | grep  "server.licence_key=*" | cut -d "=" -f 2`
	#Check if it is Enterprise or Community
	if [ -z "$get_license_key" ]; then
 	    echo "Downloading the latest version of Ant Media Server Community Edition."
    	    curl --progress-bar -o ams_community.zip -L "$(curl -s -H "Accept: application/vnd.github+json" https://api.github.com/repos/ant-media/Ant-Media-Server/releases/latest | jq -r '.assets[0].browser_download_url')"   
    	    ANT_MEDIA_SERVER_ZIP_FILE="ams_community.zip"
        else
    	   check_license=$(curl -s https://api.antmedia.io/?license="$get_license_key" | tr -d "\"")
    	   echo "Downloading the latest version of Ant Media Server Enterprise Edition."
  	   curl --progress-bar -o ams_enterprise.zip "$check_license"
  	   ANT_MEDIA_SERVER_ZIP_FILE="ams_enterprise.zip"
  	fi
        bash install_ant-media-server.sh -i $ANT_MEDIA_SERVER_ZIP_FILE -r true

}

# Exit the script, If the local version is greater than the remote version 
if [ "$(printf "%s\n" "$LOCAL_VERSION" "$REMOTE_VERSION" | sort -V | tail -n 1)" != "$LOCAL_VERSION" ]; then
    echo "There has been an error, please contact support@antmedia.io."
    exit 1
elif [ "$REMOTE_VERSION" != "$LOCAL_VERSION" ]; then
    check_ams
else
    # If the versions are equal, there is no need for an update
    echo "No update required."
fi
