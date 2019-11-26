package uk.gov.ons.census.action.config;

import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
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
  private final RabbitTemplate rabbitTemplate;
  private final ConnectionFactory connectionFactory;

  @Value("${messagelogging.logstacktraces}")
  private boolean logStackTraces;

  @Value("${queueconfig.consumers}")
  private int consumers;

  @Value("${queueconfig.retry-attempts}")
  private int retryAttempts;

  @Value("${queueconfig.retry-delay}")
  private int retryDelay;

  @Value("${queueconfig.quarantine-exchange}")
  private String quarantineExchange;

  @Value("${queueconfig.inbound-queue}")
  private String inboundQueue;

  @Value("${queueconfig.action-fulfilment-inbound-queue}")
  private String actionFulfilmentQueue;

  @Value("${queueconfig.undelivered-mail-queue}")
  private String undeliveredMailQueue;

  public MessageConsumerConfig(
      ExceptionManagerClient exceptionManagerClient,
      RabbitTemplate rabbitTemplate,
      ConnectionFactory connectionFactory) {
    this.exceptionManagerClient = exceptionManagerClient;
    this.rabbitTemplate = rabbitTemplate;
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
  public MessageChannel undeliveredMailInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public AmqpInboundChannelAdapter inbound(
      @Qualifier("container") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("caseCreatedInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public AmqpInboundChannelAdapter fulfilmentRequestInbound(
      @Qualifier("actionFulfilmentContainer")
          SimpleMessageListenerContainer actionFulfilmentContainer,
      @Qualifier("actionFulfilmentInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(actionFulfilmentContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public AmqpInboundChannelAdapter undeliveredMailInbound(
      @Qualifier("undeliveredMailContainer")
          SimpleMessageListenerContainer actionFulfilmentContainer,
      @Qualifier("undeliveredMailInputChannel") MessageChannel channel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(actionFulfilmentContainer);
    adapter.setOutputChannel(channel);
    return adapter;
  }

  @Bean
  public SimpleMessageListenerContainer container() {
    return setupListenerContainer(inboundQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer actionFulfilmentContainer() {
    return setupListenerContainer(actionFulfilmentQueue, ResponseManagementEvent.class);
  }

  @Bean
  public SimpleMessageListenerContainer undeliveredMailContainer() {
    return setupListenerContainer(undeliveredMailQueue, ResponseManagementEvent.class);
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
            queueName,
            quarantineExchange,
            rabbitTemplate);

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
}
