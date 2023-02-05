## Dockerfile

#### 1. Download Dockerfile

Text

```
wget https://raw.githubusercontent.com/ant-media/Scripts/master/docker/Dockerfile_Process -O Dockerfile
```

#### 2. Build Docker Image

Download and save Ant Media Server ZIP file in the same directory with Dockerfile. Then run the docker build command from command line

Text

```
docker build --network=host -t antmediaserver --build-arg AntMediaServer=<Replace_With_Ant_Media_Server_Zip_File> .
```

#### 3. Run the Docker Container

Now we have a docker container with Ant Media Server. Run the image.

Text

```
docker run -d --name antmedia --network=host -it antmediaserver
```

**Optional:** If you would like to use persistent volume, you can use it as follows. In this way, volume keeps even if your container is destroyed.

Text

```
docker volume create antmedia_volumedocker run -d --name antmedia --mount source=antmedia_volume,target=/usr/local/antmedia/ --network=host -it antmediaserver
```

## Docker Compose

#### 1. Download docker-compose and Dockerfile files

Text

```
wget https://raw.githubusercontent.com/ant-media/Scripts/master/docker/docker-compose.ymlwget https://raw.githubusercontent.com/ant-media/Scripts/master/docker/Dockerfile_Process -O Dockerfile
```

#### 2. Build Docker Image

Text

```none
docker-compose build --build-arg AntMediaServer=<Replace_With_Ant_Media_Server_Zip_File>
```

#### 4. Run the Docker Compose file

Text

```
docker-compose up -d
```

**Optional:** If you would like to mount an existing volume, simply change the lines below and uncomment it.

Text

```
#    volumes:#      - antmedia_vol:/usr/local/antmedia/#    volumes:#      antmedia_vol:#      external: true#      name:#      antmedia_volume
```

147 words

Published 27 Sept 2022