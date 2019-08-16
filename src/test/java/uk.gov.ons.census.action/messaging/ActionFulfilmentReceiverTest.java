package uk.gov.ons.census.action.messaging;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;

public class ActionFulfilmentReceiverTest {

  @Test
  public void testReceiveEventIgnoresUnexpectedFulfilmentCode() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseClient caseClient = mock(CaseClient.class);
    CaseRepository caseRepository = mock(CaseRepository.class);

    EasyRandom easyRandom = new EasyRandom();

    Case caze = easyRandom.nextObject(Case.class);

    when(caseRepository.findByCaseId(any(UUID.class))).thenReturn(Optional.of(caze));

    ActionFulfilmentReceiver underTest =
        new ActionFulfilmentReceiver(rabbitTemplate, caseClient, caseRepository);

    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setCaseId(UUID.randomUUID().toString());
    underTest.receiveEvent(event);
  }

  @Test
  public void testReceiveEventIgnoresUACFulfilmentCode() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseClient caseClient = mock(CaseClient.class);
    CaseRepository caseRepository = mock(CaseRepository.class);

    EasyRandom easyRandom = new EasyRandom();

    Case caze = easyRandom.nextObject(Case.class);

    when(caseRepository.findByCaseId(any(UUID.class))).thenReturn(Optional.of(caze));

    ActionFulfilmentReceiver underTest =
        new ActionFulfilmentReceiver(rabbitTemplate, caseClient, caseRepository);

    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("UACHHT1");
    event.getPayload().getFulfilmentRequest().setCaseId(UUID.randomUUID().toString());
    underTest.receiveEvent(event);
  }
}
