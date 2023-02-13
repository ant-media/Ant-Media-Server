# HLS AES encryption

HLS AES encryption refers to video streaming using HLS protocol, where the video files are encrypted by using AES-128 algorithms. There are many types of encryption algorithms, and the most common used method for HLS is AES-128.

### What is HLS AES encryption?

Advanced Encryption Standard (AES) is a block cipher that encrypts and decrypts data in 128-bit blocks. As AES is a symmetric key algorithm, there needs to be a secret key used for both encryption and decryption. That means the broadcaster encrypts the video using the key and the viewer’s browser decrypts it using the same key. Starting from 2.4, Ant Media Server supports HLS AES Encryption.

### How to use HLS AES encryption

Ant Media Server uses external ```key_info_file``` URL for segment encryption. 

Key info file format is as follows:

    key URI
    key file path
    IV (optional)

ActionScript

*   ```key URI``` specifies the key URI written to the playlist. The key URL is used to access the encryption key during playback.
*   ```key file path``` specifies the path to the key file used to obtain the key during the encryption process. The key file is read as a single packed array of 16 octets in binary format.
*   ```IV``` specifies the initialization vector (IV) as a hexadecimal string to be used instead of the segment sequence number (default) for encryption. It's an optional value.

Changes to ```key_info_file``` will result in segment encryption with the new key/IV and an entry in the playlist for the new key URI/IV if ```periodic_rekey``` is enabled in ```hls_flags```.

Below you can see a key info file example:

    http://server/file.key
    /path/to/file.key
    0123456789ABCDEF0123456789ABCDEF

ActionScript

#### **How to enable HLS AES encryption**

Let’s assume that you're already running Ant Media Server v2.4+ on your server. We’re going to use WebRTCAppEE app to enable HLS AES Encryption.

1.  Open the following file with your favorite editor.

    /usr/local/antmedia/webapps/WebRTCAppEE/WEB-INF/red5-web.properties

ActionScript

2.  Add AES Encryption URI Path to the file above. Prepare the key info file as described above. You can even use URL for specifying the key info location.

    settings.hlsEncryptionKeyInfoFile={FULL_PATH_OF_DIRECTORY}/hls_aes.keyinfo

ActionScript

For example:

    settings.hlsEncryptionKeyInfoFile={FULL_PATH_OF_DIRECTORY}/hls_aes.keyinfo

ActionScript

3.  Restart the Ant Media Server.

    sudo service antmedia restart

ActionScript

4.  Publish any stream and check ```<AMS-FOLDER>`/webapps/WebRTCAppEE/streams/streamId.m3u8``` file. You should see ```EXT-X-KEY``` parameters like below:

    #EXTM3U
    #EXT-X-VERSION:3
    #EXT-X-TARGETDURATION:2
    #EXT-X-MEDIA-SEQUENCE:16
    #EXT-X-KEY:METHOD=AES-128,URI="{keypathURI}/hls_aes.key",IV=0x00000000000000000000000000000000
    #EXTINF:1,970000,
    streamId_0p0016.ts
    #EXTINF:2,010000,
    streamId_0p0017.ts
    #EXTINF:2,050000,
    streamId_0p0018.ts
    #EXTINF:1,970000,
    streamId_0p0019.ts
    #EXTINF:2,090000,