package com.antstreaming.rtsp.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RtspDecoder extends CumulativeProtocolDecoder {
	private static Logger logger = LoggerFactory.getLogger(RtspDecoder.class);

	/**
	 * State enumerator that indicates the reached state in the RTSP message decoding process.
	 */
	public enum ReadState {
		/** Unrecoverable error occurred */
		Failed,
		/** Trying to resync */
		Sync,
		/** Waiting for a command */
		Ready,
		/** Reading interleaved packet */
		Packet,
		/** Reading command (request or command line) */
		Command,
		/** Reading headers */
		Header,
		/** Reading body (entity) */
		Body,
		/** Fully formed message */
		Dispatch
	}

	private static final Pattern rtspRequestPattern = Pattern.compile("([A-Z_]+) +([^ ]+) +RTSP/1.0");
	private static final Pattern rtspResponsePattern = Pattern.compile("RTSP/1.0 ([0-9]+) .+");
	private static final Pattern rtspHeaderPattern = Pattern
			.compile("([a-zA-Z\\-\\_]+[0-9]?):\\s?(.*)");

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
			throws Exception {
		// logger.debug("开始解码RTSP Message { ... ");
		// logger.debug("SM SEND bytes = {}", in.getHexDump());
		in.mark();
		// logger.debug("markvalue ==> " + in.markValue());
		BufferedReader reader = null;
		StringBuilder originMsg = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader(in.asInputStream(), "US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException", e);
		}

		// Retrieve status from session
		ReadState state = (ReadState) session.getAttribute("state");
		if (state == null) {
			state = ReadState.Command;
		}

		RtspMessage rtspMessage = (RtspMessage) session.getAttribute("rtspMessage");

		try {
			// logger.debug("开始解析COMMAND AND HEADER内容................");
			while (true) {
				if (state != ReadState.Command && state != ReadState.Header) {
					// the "while" loop is only used to read commands and headers
					break;
				}

				String line = reader.readLine();
				// logger.debug("line ==> " + line);
				if (line == null) {
					// there's no more data in the buffer
					break;
				}

				if (line.length() == 0) {
					originMsg.append("\r\n");
					// This is the empty line that marks the end of the headers section
					if (null == rtspMessage) {
						// state = ReadState.Command;
						continue;
					} else {
						state = ReadState.Body;
						// 保存原始请求信息
						rtspMessage.saveOriginRequest("\r\n");
						break;
					}
				}

				switch (state) {
				case Command:
					if (line.startsWith("RTSP")) { // this is a RTSP response
						Matcher m = rtspResponsePattern.matcher(line);
						if (!m.matches()) {
							session.removeAttribute("state");
							session.removeAttribute("rtspMessage");
							throw new ProtocolDecoderException("Malformed response line: " + line);
						}

						RtspCode code = RtspCode.fromString(m.group(1));
						rtspMessage = new RtspResponse();
						((RtspResponse) (rtspMessage)).setCode(code);
						RtspRequest.Verb verb = (RtspRequest.Verb) session.getAttribute("lastRequestVerb");
						((RtspResponse) (rtspMessage)).setRequestVerb(verb);
					} else { // this is a RTSP request
						Matcher m = rtspRequestPattern.matcher(line);
						if (!m.matches()) {
							session.removeAttribute("state");
							session.removeAttribute("rtspMessage");
							throw new ProtocolDecoderException("Malformed request line: " + line);
						}

						String verb = m.group(1);
						String strUrl = m.group(2);
						String url = null;
						if (!strUrl.equalsIgnoreCase("*")) {
							url = new String(strUrl);
						}

						rtspMessage = new RtspRequest();

						((RtspRequest) rtspMessage).setVerb(verb);

						if (((RtspRequest) rtspMessage).getVerb() == RtspRequest.Verb.None) {
							session.removeAttribute("state");
							session.removeAttribute("rtspMessage");
							logger.error("Invalid method: " + verb);
							throw new ProtocolDecoderException("Invalid method: " + verb);
						}
						((RtspRequest) rtspMessage).setUrl(url);
					}
					rtspMessage.saveOriginRequest(originMsg.toString() + line + "\r\n");
					state = ReadState.Header;
					break;

				case Header:
					Matcher m = rtspHeaderPattern.matcher(line);

					if (!m.matches()) {
						session.removeAttribute("state");
						session.removeAttribute("rtspMessage");
						// logger.error("RTSP header not valid line:" + line);
						throw new ProtocolDecoderException("RTSP header not valid line:" + line);
					}

					rtspMessage.setHeader(m.group(1), m.group(2));

					rtspMessage.saveOriginRequest(line + "\r\n");

					break;
				default:
					break;
				}
			}

			if (state == ReadState.Body) {
				int contentLength = Integer.parseInt(rtspMessage.getHeader("Content-Length", "0"));
				if (contentLength == 0) { // there's no buffer to be read
					state = ReadState.Dispatch;
				} else {

					int bytesToRead = contentLength - rtspMessage.getBufferSize();
					
					// read the content buffer
					CharBuffer bufferContent = CharBuffer.allocate(bytesToRead);
					reader.read(bufferContent);
					bufferContent.flip();
					rtspMessage.appendToBuffer(bufferContent);
					if (rtspMessage.getBufferSize() >= contentLength) {
						// The RTSP message parsing is completed
						state = ReadState.Dispatch;
					}
				}
			}

		} catch (IOException e) {
			/*
			 * error on input stream should not happen since the input stream is coming from a bytebuffer.
			 */
			// Exceptions.logStackTrace(e);
			e.printStackTrace();
			return false;

		} finally {
			try {
				reader.close();
			} catch (Exception e) {}
		}

		if (state == ReadState.Dispatch) {
			// The message is already formed
			// send it
			session.removeAttribute("state");
			session.removeAttribute("rtspMessage");
			out.write(rtspMessage);
			return true;
		}

		// log.debug( "INCOMPLETE MESSAGE \n" + rtspMessage );

		// Save attributes in session
		session.setAttribute("state", state);
		session.setAttribute("rtspMessage", rtspMessage);
		return false;
	}
}
