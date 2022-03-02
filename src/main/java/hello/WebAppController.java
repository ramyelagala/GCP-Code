package hello;

import hello.PubSubApplication.PubsubOutboundGateway;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import com.google.api.gax.paging.Page;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;



@RestController
public class WebAppController {

  // tag::autowireGateway[]
  @Autowired
  private PubsubOutboundGateway messagingGateway;
  // end::autowireGateway[]

  @PostMapping("/publishMessage")
  public RedirectView publishMessage(@RequestParam("message") String message) throws IOException, InterruptedException, ExecutionException {
	  String projectId = "premium-bearing-342909";
	    String topicId = "testTopic";
	    
	    GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("C:/Users/Shirisha/premium-bearing-342909-a59b8f3bd95b.json"))
	            .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
	      Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

	      System.out.println("Buckets:");
	      Page<Bucket> buckets = storage.list();
	      for (Bucket bucket : buckets.iterateAll()) {
	        System.out.println(bucket.toString());
	      }
	    
	  this.publishWithErrorHandlerExample(projectId, topicId);
	//messagingGateway.sendToPubsub(message);
	return new RedirectView("/");
  }
  
  public void publishWithErrorHandlerExample(String projectId, String topicId)
	      throws IOException, InterruptedException, ExecutionException {
	  
	    TopicName topicName = TopicName.of(projectId, topicId);
	    Publisher publisher = null;

	    try {
	      // Create a publisher instance with default settings bound to the topic
	      publisher = Publisher.newBuilder(topicName).build();

	      List<String> messages = Arrays.asList("first message", "second message");

	      for (final String message : messages) {
	        ByteString data = ByteString.copyFromUtf8(message);
	        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

	        // Once published, returns a server-assigned message id (unique within the topic)
	        ApiFuture<String> future = publisher.publish(pubsubMessage);
	        System.out.println("----------------------------------" + future.get());
	        // Add an asynchronous callback to handle success / failure
	        ApiFutures.addCallback(
	            future,
	            new ApiFutureCallback<String>() {

	              @Override
	              public void onFailure(Throwable throwable) {
	                if (throwable instanceof ApiException) {
	                  ApiException apiException = ((ApiException) throwable);
	                  // details on the API exception
	                  System.out.println(apiException.getStatusCode().getCode());
	                  System.out.println(apiException.isRetryable());
	                }
	                System.out.println("Error publishing message : " + message);
	              }

	              @Override
	              public void onSuccess(String messageId) {
	                // Once published, returns server-assigned message ids (unique within the topic)
	                System.out.println("Published message ID: " + messageId);
	              }
	            },
	            MoreExecutors.directExecutor());
	      }
	    } finally {
	      if (publisher != null) {
	        // When finished with the publisher, shutdown to free up resources.
	        publisher.shutdown();
	        publisher.awaitTermination(1, TimeUnit.MINUTES);
	      }
	    }
	  }
	

}
