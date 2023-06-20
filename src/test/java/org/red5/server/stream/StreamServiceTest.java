package org.red5.server.stream;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IStreamSecurityService;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.springframework.context.ApplicationContext;

public class StreamServiceTest {
	
	
	//Start writing test codes for StreamService because we're encountering problems here time to time. 
	
	@Test
	public void testPublishMetaData() {
		
		
		StreamService streamService = Mockito.spy(new StreamService());
		
		
		String streamId = "stream123";
		String param = "token=12345";
		String name = streamId + "?" + param;
		
		IStreamCapableConnection connection = Mockito.mock(IStreamCapableConnection.class);
		IScope scope = Mockito.mock(IScope.class);
		Mockito.when(connection.getScope()).thenReturn(scope);
		
		IContext context = Mockito.mock(IContext.class);
		Mockito.when(scope.getContext()).thenReturn(context);
		
		ApplicationContext appContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getApplicationContext()).thenReturn(appContext);
		
		IStreamSecurityService securityService = Mockito.mock(IStreamSecurityService.class);
		
		Mockito.when(appContext.containsBean(IStreamSecurityService.BEAN_NAME)).thenReturn(true);
		Mockito.when(appContext.getBean(IStreamSecurityService.BEAN_NAME)).thenReturn(securityService);
		
		
		Set<IStreamPublishSecurity> publishSecuritySet = new HashSet<>();
		
		IStreamPublishSecurity publishSecurity = Mockito.mock(IStreamPublishSecurity.class); 
		Mockito.when(publishSecurity.isPublishAllowed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);
		
		publishSecuritySet.add(publishSecurity);
		
		Mockito.when(securityService.getStreamPublishSecurity()).thenReturn(publishSecuritySet);
		
          
		Red5.setConnectionLocal(connection);
		
		Mockito.doNothing().when(streamService).sendNSFailed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		
		streamService.publish(name, null);
		
		
		Mockito.verify(streamService).sendNSFailed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		
	}

}
