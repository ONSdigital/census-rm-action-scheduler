package uk.gov.ons.census.action.messaging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@EnableRetry
public class RabbitQueueHelper {

  @Autowired private ConnectionFactory connectionFactory;

  @Autowired private RabbitTemplate rabbitTemplate;

  @Autowired private AmqpAdmin amqpAdmin;

  public void sendMessage(String exchangeName, String routingKey, Object message) {
    rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
  }

  public BlockingQueue<String> listen(String queueName) {
    BlockingQueue<String> transfer = new ArrayBlockingQueue(50);

    org.springframework.amqp.core.MessageListener messageListener =
        message -> {
          String msgStr = new String(message.getBody());
          transfer.add(msgStr);
        };

    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setMessageListener(messageListener);
    container.setQueueNames(queueName);
    container.start();

    return transfer;
  }

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 5000))
  public void sendMessage(String queueName, Object message) {

    rabbitTemplate.convertAndSend(queueName, message);
  }

  @Retryable(
      value = {java.io.IOException.class},
      maxAttempts = 10,
      backoff = @Backoff(delay = 5000))
  public void purgeQueue(String queueName) {
    amqpAdmin.purgeQueue(queueName);
  }

  public <T> T checkExpectedMessageReceived(BlockingQueue<String> queue, Class<T> theClass)
      throws InterruptedException, IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String actualMessage = queue.poll(10, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    return objectMapper.readValue(actualMessage, theClass);
  }

  public void checkNoMessagesSent(BlockingQueue<String> queue) throws InterruptedException {
    String actualMessage = queue.poll(10, TimeUnit.SECONDS);
    assertNull(actualMessage);
  }
}
