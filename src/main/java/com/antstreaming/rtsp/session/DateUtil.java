package com.antstreaming.rtsp.session;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtil {

  public static String getGmtDate() {
    DateFormat df = new SimpleDateFormat("d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    df.setTimeZone(TimeZone.getTimeZone("GMT")); // modify Time Zone.
    return (df.format(new Date()));
  }

  public static void main(String[] args) {
    System.out.println(getGmtDate());
  }

}
