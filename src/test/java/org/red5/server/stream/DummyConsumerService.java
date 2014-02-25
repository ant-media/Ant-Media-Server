package org.red5.server.stream;

import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.stream.consumer.ConnectionConsumer;

public class DummyConsumerService implements IConsumerService {

	/** {@inheritDoc} */
	public IMessageOutput getConsumerOutput(IClientStream stream) {
		IStreamCapableConnection streamConn = stream.getConnection();
		if (streamConn != null) {
			Channel data = new Channel(null, 4);
			Channel video = new Channel(null, 5);
			Channel audio = new Channel(null, 6);
			IPipe pipe = new InMemoryPushPushPipe();
			pipe.subscribe(new ConnectionConsumer(video, audio, data), null);
			return pipe;
		}
		return null;
	}

}