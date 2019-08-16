#!/bin/bash

INSTALL_DIRECTORY=/usr/local/antmedia


FULL_CHAIN_FILE=
PRIVATE_KEY_FILE=
domain=""
password=
renew_flag='false'

while getopts i:d:p:f:r option
do
  case "${option}" in
    f) FULL_CHAIN_FILE=${OPTARG};;
    p) PRIVATE_KEY_FILE=${OPTARG};;
    i) INSTALL_DIRECTORY=${OPTARG};;
    d) domain=${OPTARG};;
    r) renew_flag='true';;
   esac
done

ERROR_MESSAGE="There is a problem in installing SSL to Ant Media Server.\n Please take a look at the logs above and try to fix.\n If you do not have any idea, contact@antmedia.io"

usage() {
	echo "Usage:"
	echo "$0 -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}]"
	echo "$0 -f {FULL_CHAIN_FILE} -p {PRIVATE_KEY_FILE} -d {DOMAIN_NAME} [-i {INSTALL_DIRECTORY}]"
	echo " "
	echo "If you have any question, send e-mail to contact@antmedia.io"
}

get_password() {	
  until [ ! -z "$password" ]
  do
    read -sp 'Enter Password For SSL Certificate:' password
	if [ -z "$password" ]
	then
	  echo 
	  echo "Password cannot be empty. "
	fi
  done			   
}

SUDO="sudo"
if ! [ -x "$(command -v sudo)" ]; then
  SUDO=""
fi

delete_alias() {
  if [ -f "$1" ]; then
	 $SUDO keytool -delete -alias tomcat -storepass $password -keystore $file
	 OUT=$?
	 if [ $OUT -ne 0 ]; then
	   echo -e $ERROR_MESSAGE
	 fi
  fi
}


fullChainFileExist=false
if [ ! -z "$FULL_CHAIN_FILE" ] && [ -f "$FULL_CHAIN_FILE" ]; then
  fullChainFileExist=true
fi

privateKeyFileExist=false
if [ ! -z "$PRIVATE_KEY_FILE" ] && [ -f "$PRIVATE_KEY_FILE" ]; then
  privateKeyFileExist=true
fi

if [ "$fullChainFileExist" != "$privateKeyFileExist" ]; then
   echo "Missing full chain or private key file. Please provide both or neither of them"
   usage
   exit 1
fi

if [ ! -d "$INSTALL_DIRECTORY" ]; then
  # Control will enter here if $DIRECTORY doesn't exist.
  echo "Ant Media Server does not seem to be installed to $INSTALL_DIRECTORY"
  echo "Please install Ant Media Server with the install script or give as a parameter"
  usage
  exit 1
fi

get_new_certificate(){

if [ "$fullChainFileExist" == false ]; then
    #  install letsencrypt and get the certificate
 	echo "creating new certificate"
 	
    # Install required libraries
    $SUDO apt-get update -y -qq
    OUT=$?
    if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
    fi

    $SUDO apt-get install software-properties-common -y -qq
    OUT=$?
    if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
    fi

    $SUDO add-apt-repository ppa:certbot/certbot -y
    OUT=$?
    if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
    fi

    $SUDO apt-get update -qq -y
    OUT=$?
    if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
    fi

    $SUDO apt-get install certbot -qq -y
    OUT=$?
    if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
    fi

    #Get certificate
    $SUDO certbot certonly --standalone -d $domain
    OUT=$?
    if [ $OUT -ne 0 ]; then
      echo -e $ERROR_MESSAGE
      exit $OUT
    fi
    
    file="/etc/letsencrypt/live/$domain/keystore.jks"
    delete_alias $file
    
    file="/etc/letsencrypt/live/$domain/truststore.jks"
    delete_alias $file

    FULL_CHAIN_FILE="/etc/letsencrypt/live/$domain/fullchain.pem"
    PRIVATE_KEY_FILE="/etc/letsencrypt/live/$domain/privkey.pem"

fi
}

renew_certificate(){

   echo "renewing certificate"

   $SUDO certbot renew

   OUT=$?
   if [ $OUT -ne 0 ]; then
         echo -e $ERROR_MESSAGE
   exit $OUT
   fi
}


auth_tomcat(){
    echo ""

	TEMP_DIR=$INSTALL_DIRECTORY/$domain
	if [ ! -d "$TEMP_DIR" ]; then
	  $SUDO mkdir $TEMP_DIR
	fi


	EXPORT_P12_FILE=$TEMP_DIR/fullchain_and_key.p12
	
	DEST_KEYSTORE=$TEMP_DIR/keystore.jks
	
	TRUST_STORE=$TEMP_DIR/truststore.jks
	
	CER_FILE=$INSTALL_DIRECTORY/$domain/tomcat.cer
	
	$SUDO openssl pkcs12 -export \
	    -in $FULL_CHAIN_FILE \
	    -inkey $PRIVATE_KEY_FILE \
	    -out $EXPORT_P12_FILE \
	    -name tomcat \
	    -password pass:$password
	OUT=$?


	if [ $OUT -ne 0 ]; then
	  echo -e $ERROR_MESSAGE
	  exit $OUT
	fi
	
	
	
	$SUDO keytool -importkeystore \
	       -deststorepass $password \
	       -destkeypass $password \
	       -destkeystore $DEST_KEYSTORE \
	       -srckeystore $EXPORT_P12_FILE \
	       -srcstoretype pkcs12 \
	       -srcstorepass $password \
	       -alias tomcat \
	       -deststoretype pkcs12
	OUT=$?
	if [ $OUT -ne 0 ]; then
	  echo -e $ERROR_MESSAGE
	  exit $OUT
	fi
	
	
	$SUDO keytool -export  \
	         -alias tomcat \
	         -deststorepass $password \
	         -file $CER_FILE \
	         -keystore $DEST_KEYSTORE
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi


	$SUDO keytool -import -trustcacerts -alias tomcat \
	  -file $CER_FILE \
	  -keystore $TRUST_STORE \
	  -storepass $password -noprompt
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi
	
	
	$SUDO cp $TRUST_STORE $INSTALL_DIRECTORY/conf/
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi


	$SUDO cp $DEST_KEYSTORE $INSTALL_DIRECTORY/conf/
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi
	
	
	$SUDO sed -i "/rtmps.keystorepass=password/c\rtmps.keystorepass=$password"  $INSTALL_DIRECTORY/conf/red5.properties
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi
	
	$SUDO sed -i "/rtmps.truststorepass=password/c\rtmps.truststorepass=$password"  $INSTALL_DIRECTORY/conf/red5.properties
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi
	
	#cp default jee-container to jee-container-nossl
	$SUDO cp $INSTALL_DIRECTORY/conf/jee-container.xml $INSTALL_DIRECTORY/conf/jee-container-nossl.xml
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi

	#cp jee-container-ssl to jee-container
	$SUDO cp $INSTALL_DIRECTORY/conf/jee-container-ssl.xml $INSTALL_DIRECTORY/conf/jee-container.xml
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi
}

create_cron_job(){

	#add renew job to crontab
	$SUDO crontab -l > mycron
	
	
	#echo new cron into cron file
	#run renew script in each 85 days
	$SUDO echo "00 03 */85 * * cd $INSTALL_DIRECTORY && ./enable_ssl.sh -d $domain -r" >> mycron
	
	
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi


	#install new cron file
	$SUDO crontab mycron
	
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi

	#remove temp cron
	$SUDO rm mycron
	
	#restart cron jobs
	$SUDO systemctl restart cron
	
	OUT=$?
	if [ $OUT -ne 0 ]; then
	 echo -e $ERROR_MESSAGE
	 exit $OUT
	fi
}

generate_password(){

    #user may define his own password
    #password=$(echo -n "$domain" | sha256sum)

    password="$domain"

    echo "domain: $domain"
    #echo "generated password: $password"
}

check_domain_name(){
    #check domain name exists
    if [ -z "$domain" ]; then
    echo "Missing parameter. Domain name is not set"
    usage
    exit 1
    fi
}

#check domain name
check_domain_name

#generate password using domain name
generate_password


if [ "$renew_flag" == "true" ]
then

    #renew certificate
    renew_certificate

    #authenticate tomcat with certificate
    auth_tomcat

elif [ "$renew_flag" == "false" ]
then

    #install letsencrypt and get the certificate
    get_new_certificate

    #authenticate tomcat with certificate
    auth_tomcat

    #create cron job for auto renew
    create_cron_job
fi

echo ""

$SUDO service antmedia stop

OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

$SUDO service antmedia start

OUT=$?
if [ $OUT -ne 0 ]; then
 echo -e $ERROR_MESSAGE
 exit $OUT
fi

echo "SSL certificate is installed."
echo "Https port: 5443"
echo "You can use this url: https://$domain:5443/"

#remove temp dir
$SUDO rm -rf $TEMP_DIR
