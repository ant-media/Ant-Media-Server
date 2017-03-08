package com.antstreaming.rtsp.protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class SafeProperties extends Properties {
  private static final long serialVersionUID = 5011694856722313621L;
  private static final String keyValueSeparators = "=:\t\r\n\f"; /* 键值分隔符 */
  private static final String strictKeyValueSeparators = "=:"; /* 属性分隔符 */
  private static final String whiteSpaceChars = "\t\r\n\f"; /* 空白字符 */
  private static final String RtspLineSeparator = "\r\n"; /* RTSP行分隔符 */
  private PropertiesContext context = new PropertiesContext(); /* 属性上下文 */

  // private static final String specialSaveChars = "\t\r\n\f#!";

  public PropertiesContext getContext() {
    return context;
  }

  /**
   * load property file.
   */
  @Override
  public synchronized void load(InputStream inStream) throws IOException {
    BufferedReader in;
    in = new BufferedReader(new InputStreamReader(inStream, "8859_1"));
    while (true) {
      // Get next line
      String line = in.readLine();
      // intract property/comment string
      String intactLine = line;
      if (line == null) return;

      if (line.length() > 0) {

        // Find start of key
        int len = line.length();
        int keyStart;
        for (keyStart = 0; keyStart < len; keyStart++)
          if (whiteSpaceChars.indexOf(line.charAt(keyStart)) == -1) break;

        // Blank lines are ignored
        if (keyStart == len) continue;

        // Continue lines that end in slashes if they are not comments
        char firstChar = line.charAt(keyStart);

        if ((firstChar != '#') && (firstChar != '!')) {
          while (continueLine(line)) {
            String nextLine = in.readLine();
            intactLine = intactLine + "\n" + nextLine;
            if (nextLine == null) nextLine = "";
            String loppedLine = line.substring(0, len - 1);
            // Advance beyond whitespace on new line
            int startIndex;
            for (startIndex = 0; startIndex < nextLine.length(); startIndex++)
              if (whiteSpaceChars.indexOf(nextLine.charAt(startIndex)) == -1) break;
            nextLine = nextLine.substring(startIndex, nextLine.length());
            line = new String(loppedLine + nextLine);
            len = line.length();
          }

          // Find separation between key and value
          int separatorIndex;
          for (separatorIndex = keyStart; separatorIndex < len; separatorIndex++) {
            char currentChar = line.charAt(separatorIndex);
            if (currentChar == '\\')
              separatorIndex++;
            else if (keyValueSeparators.indexOf(currentChar) != -1) break;
          }

          // Skip over whitespace after key if any
          int valueIndex;
          for (valueIndex = separatorIndex; valueIndex < len; valueIndex++)
            if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1) break;

          // Skip over one non whitespace key value separators if any
          if (valueIndex < len)
            if (strictKeyValueSeparators.indexOf(line.charAt(valueIndex)) != -1) valueIndex++;

          // Skip over white space after other separators if any
          while (valueIndex < len) {
            if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1) break;
            valueIndex++;
          }
          String key = line.substring(keyStart, separatorIndex);
          String value = (separatorIndex < len) ? line.substring(valueIndex, len) : "";

          // Convert then store key and value
          key = loadConvert(key);
          value = loadConvert(value);
          // memorize the property also with the whold string
          put(key, value, intactLine);
        } else {
          // memorize the comment string
          context.addCommentLine(intactLine);
        }
      } else {
        // memorize the string even the string is empty
        context.addCommentLine(intactLine);
      }
    }
  }

  /*
   * Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original
   * forms
   */
  private String loadConvert(String theString) {
    // char aChar;
    // int len = theString.length();
    // StringBuffer outBuffer = new StringBuffer(len);
    //
    // for (int x = 0; x < len;) {
    // aChar = theString.charAt(x++);
    // if (aChar == '\\') {
    // aChar = theString.charAt(x++);
    // if (aChar == 'u') {
    // // Read the xxxx
    // int value = 0;
    // for (int i = 0; i < 4; i++) {
    // aChar = theString.charAt(x++);
    // switch (aChar) {
    // case '0':
    // case '1':
    // case '2':
    // case '3':
    // case '4':
    // case '5':
    // case '6':
    // case '7':
    // case '8':
    // case '9':
    // value = (value << 4) + aChar - '0';
    // break;
    // case 'a':
    // case 'b':
    // case 'c':
    // case 'd':
    // case 'e':
    // case 'f':
    // value = (value << 4) + 10 + aChar - 'a';
    // break;
    // case 'A':
    // case 'B':
    // case 'C':
    // case 'D':
    // case 'E':
    // case 'F':
    // value = (value << 4) + 10 + aChar - 'A';
    // break;
    // default:
    // throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
    // }
    // }
    // outBuffer.append((char) value);
    // } else {
    // if (aChar == 't')
    // outBuffer.append('\t'); /* ibm@7211 */
    //
    // else if (aChar == 'r')
    // outBuffer.append('\r'); /* ibm@7211 */
    // else if (aChar == 'n') {
    // /*
    // * ibm@8897 do not convert a \n to a line.separator
    // * because on some platforms line.separator is a String
    // * of "\r\n". When a Properties class is saved as a file
    // * (store()) and then restored (load()) the restored
    // * input MUST be the same as the output (so that
    // * Properties.equals() works).
    // *
    // */
    // outBuffer.append('\n'); /* ibm@8897 ibm@7211 */
    // } else if (aChar == 'f')
    // outBuffer.append('\f'); /* ibm@7211 */
    // else
    // /* ibm@7211 */
    // outBuffer.append(aChar); /* ibm@7211 */
    // }
    // } else
    // outBuffer.append(aChar);
    // }
    // return outBuffer.toString();
    return theString;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public synchronized void store(OutputStream out, String header) throws IOException {
    BufferedWriter awriter;
    awriter = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
    if (header != null) writeln(awriter, "#" + header);
    List entrys = context.getCommentOrEntrys();
    for (Iterator iter = entrys.iterator(); iter.hasNext();) {
      Object obj = iter.next();
      if (obj.toString() != null) {
        writeln(awriter, obj.toString());
      }
    }
    awriter.flush();
  }

  /**
   * 
   * @return the string 
   */
  @SuppressWarnings("rawtypes")
  public synchronized String getString() {
    StringBuffer sb = new StringBuffer();
    List entrys = context.getCommentOrEntrys();

    Object obj;
    for (Iterator iter = entrys.iterator(); iter.hasNext();) {
      obj = iter.next();
      if (obj.toString() != null) {
        sb.append(obj.toString()).append(RtspLineSeparator);
      }
      obj = null;
    }

    return sb.toString();
  }

  private static void writeln(BufferedWriter bw, String s) throws IOException {
    bw.write(s);
    bw.newLine();
  }

  private boolean continueLine(String line) {
    int slashCount = 0;
    int index = line.length() - 1;
    while ((index >= 0) && (line.charAt(index--) == '\\'))
      slashCount++;
    return (slashCount % 2 == 1);
  }

  /*
   * Converts unicodes to encoded &#92;uxxxx and writes out any of the characters in
   * specialSaveChars with a preceding slash
   */
  private String saveConvert(String theString, boolean escapeSpace) {
    // int len = theString.length();
    // StringBuffer outBuffer = new StringBuffer(len * 2);
    //
    // for (int x = 0; x < len; x++) {
    // char aChar = theString.charAt(x);
    // switch (aChar) {
    // // case ' ':
    // // if (x == 0 || escapeSpace)
    // // outBuffer.append('\\');
    // //
    // // outBuffer.append(' ');
    // // break;
    // case '\\':
    // outBuffer.append('\\');
    // outBuffer.append('\\');
    // break;
    // case '\t':
    // outBuffer.append('\\');
    // outBuffer.append('t');
    // break;
    // case '\n':
    // outBuffer.append('\\');
    // outBuffer.append('n');
    // break;
    // case '\r':
    // outBuffer.append('\\');
    // outBuffer.append('r');
    // break;
    // case '\f':
    // outBuffer.append('\\');
    // outBuffer.append('f');
    // break;
    // default:
    // if ((aChar < 0x0020) || (aChar > 0x007e)) {
    // outBuffer.append('\\');
    // outBuffer.append('u');
    // outBuffer.append(toHex((aChar >> 12) & 0xF));
    // outBuffer.append(toHex((aChar >> 8) & 0xF));
    // outBuffer.append(toHex((aChar >> 4) & 0xF));
    // outBuffer.append(toHex(aChar & 0xF));
    // } else {
    // if (specialSaveChars.indexOf(aChar) != -1)
    // outBuffer.append('\\');
    // outBuffer.append(aChar);
    // }
    // }
    // }
    return theString;
  }


  // private static char toHex(int nibble) {
  // return hexDigit[(nibble & 0xF)];
  // }

  // /** A table of hex digits */
  // private static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
  // 'B', 'C', 'D', 'E',
  // 'F' };

  @Override
  public synchronized Object put(Object key, Object value) {
    context.putOrUpdate(key.toString(), value.toString());
    return super.put(key, value);
  }

  public synchronized Object put(Object key, Object value, String line) {
    context.putOrUpdate(key.toString(), value.toString(), line);
    return super.put(key, value);
  }

  @Override
  public synchronized Object remove(Object key) {
    context.remove(key.toString());
    return super.remove(key);
  }

  /**
   * Description:属性上下文.
   */
  class PropertiesContext {
    // 保存属性或注释容器类.
    @SuppressWarnings("rawtypes")
    private List commentOrEntrys = new ArrayList();

    @SuppressWarnings("rawtypes")
    public List getCommentOrEntrys() {
      return commentOrEntrys;
    }

    @SuppressWarnings("unchecked")
    public void addCommentLine(String line) {
      commentOrEntrys.add(line);
    }

    @SuppressWarnings("unchecked")
    public void putOrUpdate(PropertyEntry pe) {
      remove(pe.getKey());
      commentOrEntrys.add(pe);
    }

    @SuppressWarnings("unchecked")
    public void putOrUpdate(String key, String value, String line) {
      PropertyEntry pe = new PropertyEntry(key, value, line);
      remove(key);
      commentOrEntrys.add(pe);
    }

    @SuppressWarnings("unchecked")
    public void putOrUpdate(String key, String value) {
      PropertyEntry pe = new PropertyEntry(key, value);
      remove(key);
      commentOrEntrys.add(pe);
    }

    public void remove(String key) {
      for (int i = 0; i < commentOrEntrys.size(); i++) {
        Object obj = commentOrEntrys.get(i);
        if (obj instanceof PropertyEntry) {
          if (obj != null) {
            if (key.equals(((PropertyEntry) obj).getKey())) {
              commentOrEntrys.remove(obj);
            }
          }
        }
      }
    }

    /**
     * @param string
     */
    public void addComment(String comment) {
      if (comment != null) {
        context.addCommentLine("#" + comment);
      }
    }

    class PropertyEntry {
      private String key;
      private String value;
      private String line;

      /**
       * 构造函数.
       * 
       * @param key
       * @param value
       */
      public PropertyEntry(String key, String value) {
        this.key = key;
        this.value = value;
      }

      /**
       * 构造函数.
       * 
       * @param key
       * @param value
       */
      public PropertyEntry(String key, String value, String line) {
        this(key, value);
        this.line = line;
      }

      /* ~~~~~~~~~~~ Setters And Getters ~~~~~~~~~~~~~~~~~~ */
      public String getKey() {
        return key;
      }

      public void setKey(String key) {
        this.key = key;
      }

      public String getValue() {
        return value;
      }

      public void setValue(String value) {
        this.value = value;
      }

      public String getLine() {
        return line;
      }

      public void setLine(String line) {
        this.line = line;
      }

      @Override
      public String toString() {
        if (line != null) {
          return line;
        }
        if (key != null && value != null) {
          String k = saveConvert(key, true);
          String v = saveConvert(value, false);
          return k + ":" + v;
        }
        return null;
      }
    }
  }
}
