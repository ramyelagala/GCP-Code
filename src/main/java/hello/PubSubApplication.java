package hello;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

@SpringBootApplication
public class PubSubApplication {

  private static final Log LOGGER = LogFactory.getLog(PubSubApplication.	class);

  public static void main(String[] args) throws IOException {
	SpringApplication.run(PubSubApplication.class, args);
  }

    // Inbound channel adapter.

  // tag::pubsubInputChannel[]
  @Bean
  public MessageChannel pubsubInputChannel() {
	return new DirectChannel();
  }
  // end::pubsubInputChannel[]

  // tag::messageChannelAdapter[]
  @Bean
  public PubSubInboundChannelAdapter messageChannelAdapter(
	  @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
	  PubSubTemplate pubSubTemplate) {
	PubSubInboundChannelAdapter adapter =
		new PubSubInboundChannelAdapter(pubSubTemplate, "testTopic-sub");
	adapter.setOutputChannel(inputChannel);
	adapter.setAckMode(AckMode.MANUAL);

	return adapter;
  }
  // end::messageChannelAdapter[]

  // tag::messageReceiver[]
  @Bean
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public MessageHandler messageReceiver() {
	return message -> {
	  LOGGER.info("Message arrived! Payload: " + new String((byte[]) message.getPayload()));
	  BasicAcknowledgeablePubsubMessage originalMessage =
		message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
	  originalMessage.ack();
	  LOGGER.info("Message arrived! Acknowledge: " + new String((byte[]) originalMessage.ack().toString().getBytes()));
	};
  }
  // end::messageReceiver[]

  // Outbound channel adapter

  // tag::messageSender[]
  @Bean
  @ServiceActivator(inputChannel = "pubsubOutputChannel")
  public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
	return new PubSubMessageHandler(pubsubTemplate, "testTopic");
  }
  // end::messageSender[]

  // tag::messageGateway[]
  @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
  public interface PubsubOutboundGateway {

	void sendToPubsub(String text);
  }
  // end::messageGateway[]
}
