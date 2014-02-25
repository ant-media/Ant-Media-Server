#!/bin/sh
# Copyright (C) 2008 Vlideshow Inc., All Rights Reservered
# http://www.theyard.net/ or http://www.vlideshow.com/
#
# This library is free software; you can redistribute it and/or modify it under the 
# terms of the GNU Lesser General Public License as published by the Free Software 
# Foundation; either version 2.1 of the License, or (at your option) any later 
# version. 
# 
# This library is distributed in the hope that it will be useful, but WITHOUT ANY 
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
# PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public License along 
# with this library; if not, write to the Free Software Foundation, Inc., 
# 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
#
#
#  author: trebor
#  author: aclarke
# created: 14 august 2008
#
# Contains a bunch of different shell tools that are used in
# oru scripts
#


# detect the current os, adjust and simpify the name, and set the $OS
# variable to that value
#
# to use put something like this in your code:
# 
#   . shell-tools.sh
#   detect_os
#   echo $OS
detect_os()
{
  OS=`uname`
  case "$OS" in
    CYGWIN_NT-6*)
      OS=cygwin6
      ;;
    CYGWIN_NT-5*)
      OS=cygwin5
      ;;
    Darwin*)
      OS=darwin
      ;;
    Linux*)
      OS=linux
      ;;
    *)
      echo "Could not detect OS"
      exit 1
      ;;
  esac
}

# Checks the status passed in, and if not 0, outputs the
# message in the second argument and exits
#
# To use do the following
# $ command_i_want_to_check
# $ process_status $? "command_i_want_to_check failed with code: $?"
process_status()
{
  if [ $1 -ne 0 ]; then
    echo "process failed: $2"
    exit -1;
  fi
}

# Checks if the passed in PID is running as a background process
# If not, it exits the process, first displaying the message given
#
# To use:
# $ background_process &
# $ ensure_background_process_running $! 5 "background_process doesn't appear to be running"
ensure_background_process_running()
{
  _EBPS_PID=$1
  _EBPS_SLEEPTIMEOUT=$2
  _EBPS_FAILUREMSG=$3
  sleep $_EBPS_SLEEPTIMEOUT
  ps -ef | grep -v grep | grep -q $_EBPS_PID
  if [ $? -ne 0 ]; then
    echo $_EBPS_FAILUREMSG
    # Just in case, send a kill signal
    /bin/kill $_EBPS_FAILUREMSG >/dev/null 2>&1
    # And reap the failed child
    wait $_EBPS_PID
    exit 1
  fi
}
