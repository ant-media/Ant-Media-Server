Copyright (C) Vlideshow Inc., All Rights Reserved

Some of the headless things we do actually require an X server
(e.g. flash) to actually work; they don't like having no screen.

So, before this leaves my head, here's what you have to do for that:

*) First, make sure you can run all the tests in a non-headless mode
  -) Build all the necessary red5 applications
  -) Make sure $VLIDESHOW_HOME is defined
  -) Make sure $VLIDESHOW_HOME/share/vlideshow/scripts is in your path
  -) Make sure red5 is started by calling: red5 restart
  -) Make sure you have flash installed, and a debug player in your
     path as flashplayer-debug
  -) Make sure you have a mm.cfg file configured for flash
  -) Build the flash application you want to tests
  -) Run the tests once (ant run-tests), and configure the tests to always
     have access to your camera/mic.  Check the remember checkbox
     to on (this is needed to make sure we can run camera/mic tests
     without having a user give permission on every run).
  -) Go to the Adobe Global Flash Security settings web page, and
     make sure Flash trusts any applications that are in $HOME
     or below (this is needed to make sure the flash player can
     exit in headless mode).
*) Make sure you have Xvfb installed
*) Make sure you have enabled (via xauth) Xvfb to create display :1
  This command works assuming you've once logged onto an actual X Server
  on display :0 on this machine:
      xauth add "$(/bin/hostname)/unix:1" MIT-MAGIC-COOKIE-1 \
        $( xauth list | egrep "$(/bin/hostname)/unix:0" | awk '{print $3}' )
*) Now, try "ant run-tests-unattended"

