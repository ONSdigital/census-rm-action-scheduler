package uk.gov.ons.census.action.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import uk.gov.ons.census.action.client.ExceptionManagerClient;
import uk.gov.ons.census.action.messaging.ManagedMessageRecoverer;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;

@Configuration
public class MessageConsumerConfig {
  private final ExceptionManagerClient exceptionManagerClient;
  private final ConnectionFactory connectionFactory;

  @Value("${messagelogging.logstacktraces}")
  private boolean logStackTraces;

  @Value("${queueconfig.consumers}")
  private int consumers;

  @Value("${queueconfig.retry-attempts}")
  private int retryAttempts;

  @Value("${queueconfig.retry-delay}")
  private int retryDelay;

  @Value("${queueconfig.inbound-queue}")
  private String inboundQueue;

  @Value("${queueconfig.action-fulfilment-inbound-queue}")
  private String actionFulfilmentQueue;

  public MessageConsumerConfig(
      ExceptionManagerClient exceptionManagerClient, ConnectionFactory connectionFactory) {
    this.exceptionManagerClient = exceptionManagerClient;
    this.connectionFactory = connectionFactory;
  }

  @Bean
  public MessageChannel caseCreatedInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel actionFulfilmentInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public AmqpInboundChannelAdapter inbound(
      @Qualifier("container") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("caseCreatedInputChannel") MessageChannel channel) {
    return makeAdapter(listenerContainer, channel);
  }

  @Bean
  public AmqpInboundChannelAdapter fulfilmentRequestInbound(
      @Qualifier("actionFulfilmentContainer")
          SimpleMessageListenerContainer actionFulfilmentContainer,
      @Qualifier("actionFulfilmentInputChannel") MessageChannel channel) {
    return makeAdapter(actionFulfilmentContainer, channel);
  }

  @Bean
  public SimpleMessageListenerContainer container() {
    return setupListenerContainer(inboundQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer actionFulfilmentContainer() {
    return setupListenerContainer(actionFulfilmentQueue, ResponseManagementEvent.class);
  }

  private SimpleMessageListenerContainer setupListenerContainer(
      String queueName, Class expectedMessageType) {
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(retryDelay);

    ManagedMessageRecoverer managedMessageRecoverer =
        new ManagedMessageRecoverer(
            exceptionManagerClient,
            expectedMessageType,
            logStackTraces,
            "Action Scheduler",
            queueName);

    RetryOperationsInterceptor retryOperationsInterceptor =
        RetryInterceptorBuilder.stateless()
            .maxAttempts(retryAttempts)
            .backOffPolicy(fixedBackOffPolicy)
            .recoverer(managedMessageRecoverer)
            .build();

    SimpleMessageListenerContainer container =
        new SimpleMessageListenerContainer(connectionFactory);
    container.setQueueNames(queueName);
    container.setConcurrentConsumers(consumers);
    container.setChannelTransacted(true);
    container.setAdviceChain(retryOperationsInterceptor);
    return container;
  }

  private AmqpInboundChannelAdapter makeAdapter(
      AbstractMessageListenerContainer listenerContainer, MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    adapter.setHeaderMapper(
        new DefaultAmqpHeaderMapper(null, null) {
          @Override
          public Map<String, Object> toHeadersFromRequest(MessageProperties source) {
            Map<String, Object> headers = new HashMap<>();

            /* We strip EVERYTHING out of the headers and put the content type back in because we
             * don't want the __TYPE__ to be processed by Spring Boot, which would cause
             * ClassNotFoundException because the type which was sent doesn't match the type we
             * want to receive. This is an ugly workaround, but it works.
             */
            headers.put("contentType", "application/json");
            return headers;
          }
        });
    return adapter;
  }
}
