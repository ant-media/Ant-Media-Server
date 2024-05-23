#!/bin/bash

# -----------------------------------------------------------------------------
# upgrade.sh - Update Script
# -----------------------------------------------------------------------------

# Description:
#   This script facilitates the seamless update of Ant Media Server to the latest version,
#   ensuring you have the most up-to-date features, improvements, and bug fixes. 
#   For more information: https://github.com/ant-media/Ant-Media-Server/releases

# Usage:
#   1. Run the script directly in the terminal using "./upgrade.sh".
#   2. Utilize the dashboard's integrated update feature for a user-friendly experience.

# Note:
#   - Ensure internet connectivity for downloading the latest Ant Media Server release.

# -----------------------------------------------------------------------------


INSTALL_DIRECTORY=$(dirname "$0")
cd $INSTALL_DIRECTORY


REMOTE_VERSION=$(curl -s https://antmedia.io/download/latest-version.json | jq -r ".versionName")
LOCAL_VERSION=$(unzip -p $INSTALL_DIRECTORY/ant-media-server.jar | grep -a "Implementation-Version"|cut -d' ' -f2 | tr -d '\r')
GITHUB_LATEST_VERSION=$(curl -s -H "Accept: application/vnd.github+json" https://api.github.com/repos/ant-media/Ant-Media-Server/releases/latest | jq -r '.tag_name' | cut -d 'v' -f 2)

# If not installed jq package, install it
if [ ! command -v jq &> /dev/null ]; then
    sudo apt-get install -y jq
fi

# Check if a variable DIR is provided (this is for the automation)
if [ -n "$DIR" ]; then
    INSTALL_DIRECTORY="$DIR"
fi


check_ams() {

	#Download the latest version of the installation script
	wget -O install_ant-media-server.sh https://raw.githubusercontent.com/ant-media/Scripts/master/install_ant-media-server.sh && sudo chmod 755 install_ant-media-server.sh
	get_license_key=`cat $INSTALL_DIRECTORY/conf/red5.properties  | grep  "server.licence_key=*" | cut -d "=" -f 2`
	#Check if it is Enterprise or Community
	if [ -z "$get_license_key" ]; then
	
	   #if there is no license key, it can be still enterprise edition in the marketplace
	   
	   if find "$INSTALL_DIRECTORY/plugins/" -type f -name "ant-media-enterprise*" | grep -q .; then
		  # there is enterprise file in plugins directory, check that if it's marketplace edition
		  echo "It seems like an enterprise version. On the other hand, there is no license key. If this is a Cloud Marketplace build, upgrade your version through your Cloud Marketplace"
	      exit 1
       elif [ "$GITHUB_LATEST_VERSION" == "$REMOTE_VERSION" ]; then
         echo "Downloading the latest version of Ant Media Server Community Edition..."
  		 curl --progress-bar -o ams_community.zip -L "$(curl -s -H "Accept: application/vnd.github+json" https://api.github.com/repos/ant-media/Ant-Media-Server/releases/latest | jq -r '.assets[0].browser_download_url')"   
    	 ANT_MEDIA_SERVER_ZIP_FILE="ams_community.zip"
	   else
      	 exit 1
       fi
  
    else
    	
      check_license=$(curl -s https://api.antmedia.io/?license="$get_license_key" | tr -d "\"")
            
       if [[ ! $check_license =~ ^http ]]; then
       	
		   if [ "$check_license" == "400" ] || [ "$check_license" == "401" ]; then
				echo "Invalid license key. Please check your license key."
			else 
				echo "Unexpected response from service: $check_license. Please try again later";
			fi
		  	exit 1
		  	
	   else
		  echo "Downloading the latest version of Ant Media Server Enterprise Edition..."
		  curl --progress-bar -o ams_enterprise.zip "$check_license"
		  ANT_MEDIA_SERVER_ZIP_FILE="ams_enterprise.zip"
		fi
	 
  	fi
  

    bash install_ant-media-server.sh -i $ANT_MEDIA_SERVER_ZIP_FILE -r true

}

# Exit the script, If the local version is greater than the remote version 
if [ "$(printf "%s\n" "$LOCAL_VERSION" "$REMOTE_VERSION" | sort -V | tail -n 1)" = "$LOCAL_VERSION" ]; then
    echo "It's already up-to-date. No need to update"
elif [ "$REMOTE_VERSION" != "$LOCAL_VERSION" ]; then
    check_ams
else
    # If the versions are equal, there is no need for an update
    echo "There has been an error, please contact support@antmedia.io."
    exit 1
fi
