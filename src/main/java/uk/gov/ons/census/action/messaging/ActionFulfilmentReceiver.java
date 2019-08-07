package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.*;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseState;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

import java.util.Optional;
import java.util.UUID;

@MessageEndpoint
public class ActionFulfilmentReceiver {
    private static final Logger log = LoggerFactory.getLogger(ActionFulfilmentReceiver.class);
    private final RabbitTemplate rabbitTemplate;

    @Value("${queueconfig.outbound-exchange}")
    private String outboundExchange;

    public ActionFulfilmentReceiver(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    @ServiceActivator(inputChannel = "actionFulfilmentInputChannel")
    public void receiveEvent(ResponseManagementEvent responseManagementEvent) {
        var event = responseManagementEvent.getEvent().getType();

        PrintFileDto printFileDto = new PrintFileDto();


        rabbitTemplate.convertAndSend(outboundExchange, "Action.Printer.binding", printFileDto);


    }


}
