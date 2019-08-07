package uk.gov.ons.census.action.config;

import static org.springframework.amqp.core.Binding.DestinationType.QUEUE;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueSetterUpper {
  @Value("${queueconfig.inbound-queue}")
  private String inboundQueue;

  @Value("${queueconfig.outbound-printer-queue}")
  private String outboundPrinterQueue;

  @Value("${queueconfig.outbound-field-queue}")
  private String outboundFieldQueue;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.outbound-printer-routing-key}")
  private String outboundPrinterRoutingKey;

  @Value("${queueconfig.outbound-field-routing-key}")
  private String outboundFieldRoutingKey;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  @Value("${queueconfig.action-case-queue}")
  private String actionCaseQueue;

  @Value("${queueconfig.action-fulfilment-inbound-queue}")
  private String actionFulfilmentQueue;

  @Value("${queueconfig.events-exchange}")
  private String eventsExchange;

  @Value("${queueconfig.events-fulfilment-request-binding}")
  private String eventsFulfilmentRequestBinding;

  @Bean
  public Queue inboundQueue() {
    return new Queue(inboundQueue, true);
  }

  @Bean
  public Queue outboundPrinterQueue() {
    return new Queue(outboundPrinterQueue, true);
  }

  @Bean
  public Queue outboundFieldQueue() {
    return new Queue(outboundFieldQueue, true);
  }

  @Bean
  public Queue actionCaseQueue() {
    return new Queue(actionCaseQueue, true);
  }

  @Bean
  public Queue actionFulfilmentQueue() {
    return new Queue(actionFulfilmentQueue, true);
  }

  @Bean
  public DirectExchange outboundExchange() {
    return new DirectExchange(outboundExchange, true, false);
  }

  @Bean
  public DirectExchange actionCaseExchange() {
    return new DirectExchange(actionCaseExchange, true, false);
  }

  @Bean
  public Exchange eventsExchange() {
    return new TopicExchange(eventsExchange, true, false);
  }

  @Bean
  public Binding actionCaseBinding() {
    return new Binding(actionCaseQueue, QUEUE, actionCaseExchange, "", null);
  }

  @Bean
  public Binding printerBinding() {
    return new Binding(
        outboundPrinterQueue, QUEUE, outboundExchange, outboundPrinterRoutingKey, null);
  }

  @Bean
  public Binding fieldBinding() {
    return new Binding(outboundFieldQueue, QUEUE, outboundExchange, outboundFieldRoutingKey, null);
  }

  @Bean
  public Binding eventsBinding() {
    return new Binding(actionFulfilmentQueue, QUEUE, eventsExchange, eventsFulfilmentRequestBinding, null);
  }
}
