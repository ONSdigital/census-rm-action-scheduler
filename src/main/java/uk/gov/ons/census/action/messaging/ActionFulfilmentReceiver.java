package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.service.CaseService;

@MessageEndpoint
public class ActionFulfilmentReceiver {
    private static final Logger log = LoggerFactory.getLogger(ActionFulfilmentReceiver.class);
    private final RabbitTemplate rabbitTemplate;
    private final CaseService caseService;

    @Value("${queueconfig.outbound-exchange}")
    private String outboundExchange;

    @Value("${queueconfig.outbound-printer-routing-key}")
    private String outboundPrinterRoutingKey;


    public ActionFulfilmentReceiver(
            RabbitTemplate rabbitTemplate,
            CaseService caseService) {
        this.rabbitTemplate = rabbitTemplate;
        this.caseService = caseService;
    }

    @Transactional
    @ServiceActivator(inputChannel = "actionFulfilmentInputChannel")
    public void receiveEvent(ResponseManagementEvent responseManagementEvent) {
        var event = responseManagementEvent.getEvent().getType();

        PrintFileDto printFileDto = new PrintFileDto();

        UacQidDTO uacQid = caseService.getUacQid("1");
        printFileDto.setQid(uacQid.getQid());
        printFileDto.setUac(uacQid.getUac());
        rabbitTemplate.convertAndSend(outboundExchange, outboundPrinterRoutingKey, printFileDto);

    }


}
