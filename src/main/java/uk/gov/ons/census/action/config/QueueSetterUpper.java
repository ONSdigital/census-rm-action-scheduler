package uk.gov.ons.census.action.config;

import static org.springframework.amqp.core.Binding.DestinationType.QUEUE;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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

  @Value("${queueconfig.action-case-exchange")
  private String actionCaseExchange;

  @Value("${queueconfig.action-case-queue")
  private String actionCaseQueue;

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
  public DirectExchange outboundExchange() {
    return new DirectExchange(outboundExchange, true, false);
  }

  @Bean
  public DirectExchange actionCaseExchange() {
    return new DirectExchange(actionCaseExchange, true, false);
  }

  @Bean
  public Binding actionCaseBinding() {
    return new Binding(
        actionCaseQueue, QUEUE, actionCaseExchange, "", null);
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
}
