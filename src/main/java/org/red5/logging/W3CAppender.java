/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.red5.server.api.Red5;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.status.ErrorStatus;

/**
 * Logback appender for the Extended W3C format.
 * 
 * @see "http://www.w3.org/TR/WD-logfile.html"
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class W3CAppender extends FileAppender<LoggingEvent> {

    /**
     * #Software: Red5 0.7.1 #Version: 1.0 #Date: 1998-11-19 22:48:39 #Fields: date time c-ip cs-username s-ip cs-method
     */

    //whether or not the header has been written
    private static boolean headerWritten;

    //events that are to be logged
    private static String events;

    //linked list to preserve order
    private static List<String> eventsList = new ArrayList<>();

    //fields that are to be logged
    private static String fields;

    //linked list to preserve order
    private static LinkedList<String> fieldList = new LinkedList<>();

    public W3CAppender() {
        setPrudent(true);
    }

    public void setEvents(String events) {
        W3CAppender.events = events;
        //make a list out of the event names
        String[] arr = events.split(";");
        for (String s : arr) {
            eventsList.add(s);
        }
    }

    public String getEvents() {
        return events;
    }

    public void setFields(String fields) {
        W3CAppender.fields = fields;
        //make a list out of the field names
        String[] arr = fields.split(";");
        for (String s : arr) {
            fieldList.add(s);
        }
    }

    public String getFields() {
        return fields;
    }

    @Override
    public synchronized void doAppend(LoggingEvent event) {
        //get the log message
        String message = event.getFormattedMessage();
        //look for w3c prefix
        if (!message.startsWith("W3C")) {
            return;
        }
        // http://logback.qos.ch/apidocs/ch/qos/logback/classic/spi/LoggingEvent.html
        StringBuilder sbuf = new StringBuilder(128);
        //see if header has been written
        if (!headerWritten) {
            //build the header
            StringBuilder sb = new StringBuilder("#Software: ");
            sb.append(Red5.VERSION);
            sb.append("\n#Version: 1.0");
            sb.append("\n#Date: ");
            sb.append(new Date());
            sb.append("\n#Fields: ");
            for (String field : fields.split(";")) {
                sb.append(field);
                sb.append(' ');
            }
            sb.append('\n');
            sbuf.append(sb.toString());
            headerWritten = true;
            sb = null;
        }
        //break the message into pieces
        String[] arr = message.split(" ");
        //create a map
        Map<String, String> elements = new HashMap<>(arr.length);
        int i = 0;
        for (String s : arr) {
            if ((i = s.indexOf(':')) != -1) {
                String key = s.substring(0, i);
                String value = s.substring(i + 1);
                elements.put(key, value);
            }
        }
        //Events					Categories
        //connect-pending			session
        //connect					session                     
        //disconnect                session                     
        //publish                   stream                         
        //unpublish                 stream                  
        //play                      stream                       
        //pause                     stream                     
        //unpause                   stream                      
        //seek                      stream                              
        //stop                      stream                       
        //record                    stream                              
        //recordstop                stream                              
        //server-start              server                              
        //server-stop               server                              
        //vhost-start               vhost                               
        //vhost-stop                vhost                               
        //app-start                 application                         
        //app-stop                  application    
        //filter based on event type - asterik allows all events
        if (!events.equals("*")) {
            if (!eventsList.contains(elements.get("x-event"))) {
                elements.clear();
                elements = null;
                sbuf = null;
                return;
            }
        }
        //x-category		event category		
        //x-event			type of event
        //date				date at which the event occurred
        //time				time at which the event occurred
        //tz               	time zone information                           
        //x-ctx            	event dependant context information             
        //s-ip		        ip address[es] of the server                    
        //x-pid            	server process id                               
        //x-cpu-load       	cpu load                                        
        //x-mem-load       	memory load (as reported in getServerStats)     
        //x-adaptor        	adaptor name                                    
        //x-vhost          	vhost name                                      
        //x-app	          	application name                                
        //x-appinst        	application instance name                       
        //x-duration	    duration of an event/session                    
        //x-status		    status code					                            
        //c-ip             	client ip address                               
        //c-proto          	connection protocol - rtmp or rtmpt             
        //s-uri            	uri of the fms application                      
        //cs-uri-stem      	stem of s-uri                                   
        //cs-uri-query     	query portion of s-uri                          
        //c-referrer       	uri of the referrer                             
        //c-user-agent     	user agent                                      
        //c-client-id      	client id                                       
        //cs-bytes         	bytes transferred from client to server         
        //sc-bytes         	bytes transferred from server to client         
        //c-connect-type   	type of connection received by the server       
        //x-sname          	stream name                                     
        //x-sname-query    	query portion of stream uri                     
        //x-suri-query		same as x-sname-query              	
        //x-suri-stem		cs-uri-stem + x-sname + x-file-ext       	
        //x-suri			x-suri-stem + x-suri-query         
        //x-file-name      	full file path of recorded stream               
        //x-file-ext       	stream type (flv or mp3)                        
        //x-file-size      	stream size in bytes                            
        //x-file-length    	stream length in seconds                        
        //x-spos           	stream position                                 
        //cs-stream-bytes  	stream bytes transferred from client to server  
        //sc-stream-bytes  	stream bytes transferred from server to client  
        //x-service-name   	name of the service providing the connection    
        //x-sc-qos-bytes	bytes transferred from server to client for quality of service
        //x-comment	      	comments
        //we may need date and/or time
        Calendar cal = GregorianCalendar.getInstance();
        cal.clear();
        cal.setTimeInMillis(event.getTimeStamp());
        //loop through the field names and grab the values from the map
        //fields without a value get a tab character as a place holder if
        //their value is not available to the server
        for (String field : fieldList) {
            String value = elements.get(field);
            if (value == null) {
                if ("date".equals(field)) {
                    sbuf.append(cal.get(Calendar.MONTH) + 1);
                    sbuf.append('/');
                    sbuf.append(cal.get(Calendar.DAY_OF_MONTH));
                    sbuf.append('/');
                    sbuf.append(cal.get(Calendar.YEAR));
                } else if ("time".equals(field)) {
                    sbuf.append(cal.get(Calendar.HOUR_OF_DAY));
                    sbuf.append(':');
                    int min = cal.get(Calendar.MINUTE);
                    if (min < 10) {
                        sbuf.append('0');
                        sbuf.append(min);
                    } else {
                        sbuf.append(min);
                    }
                } else if ("s-ip".equals(field)) {
                    //where should we grab the server ip from?
                    sbuf.append("127.0.0.1");
                } else if ("x-pid".equals(field)) {
                    //should we pass thread name?
                    sbuf.append(event.getThreadName());
                } else {
                    sbuf.append('\t');
                }
            } else {
                sbuf.append(value);
            }
            //space padded
            sbuf.append(' ');
        }
        sbuf.append('\n');
        try {
            //switch out the message
            event.setMessage(sbuf.toString());
            //write it
            writeOut(event);
        } catch (IOException ioe) {
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }

}
