package uk.gov.ons.census.action.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import uk.gov.ons.census.action.builders.FieldworkFollowupBuilder;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.FulfilmentInformation;
import uk.gov.ons.census.action.model.dto.Payload;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

public class UndeliveredMailReceiverTest {
  @Test
  public void testHappyPathWithQid() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    UUID testCaseId = UUID.randomUUID();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCaseId.toString());
    Case caze = new Case();
    caze.setFieldCoordinatorId("Noodlebiscuits");
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(uacQidLinkRepository.findByQid(eq("testQid"))).thenReturn(Optional.of(uacQidLink));
    when(caseRepository.findByCaseId(eq(testCaseId))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setQuestionnaireId("testQid");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(uacQidLinkRepository).findByQid(eq("testQid"));
    verify(caseRepository).findByCaseId(eq(testCaseId));
    verify(fieldworkFollowupBuilder)
        .buildFieldworkFollowup(eq(caze), eq("dummy"), eq("dummy"), eq(true));
    verify(rabbitTemplate).convertAndSend(any(), eq("Action.Field.binding"), eq(fieldworkFollowup));
  }

  @Test
  public void testHappyPathWithCaseRef() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    Case caze = new Case();
    caze.setFieldCoordinatorId("Noodlebiscuits");
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(caseRepository.findById((eq(123)))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setCaseRef("123");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(uacQidLinkRepository, never()).findByQid(any());
    verify(caseRepository).findById(eq(123));
    verify(fieldworkFollowupBuilder)
        .buildFieldworkFollowup(eq(caze), eq("dummy"), eq("dummy"), eq(true));
    verify(rabbitTemplate).convertAndSend(any(), eq("Action.Field.binding"), eq(fieldworkFollowup));
  }

  @Test
  public void testReceiptReceivedIsIgnored() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    Case caze = new Case();
    caze.setFieldCoordinatorId("Noodlebiscuits");
    caze.setReceiptReceived(true);
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(caseRepository.findById((eq(123)))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setCaseRef("123");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(rabbitTemplate, never())
        .convertAndSend(any(), anyString(), any(FieldworkFollowup.class));
  }

  @Test
  public void testRefusalReceivedIsIgnored() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    Case caze = new Case();
    caze.setFieldCoordinatorId("Noodlebiscuits");
    caze.setRefusalReceived(true);
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(caseRepository.findById((eq(123)))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setCaseRef("123");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(rabbitTemplate, never())
        .convertAndSend(any(), anyString(), any(FieldworkFollowup.class));
  }

  @Test
  public void testAddressInvalidIsIgnored() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    Case caze = new Case();
    caze.setFieldCoordinatorId("Noodlebiscuits");
    caze.setAddressInvalid(true);
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(caseRepository.findById((eq(123)))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setCaseRef("123");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(rabbitTemplate, never())
        .convertAndSend(any(), anyString(), any(FieldworkFollowup.class));
  }

  @Test
  public void testHiCaseIsIgnored() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    Case caze = new Case();
    caze.setFieldCoordinatorId("Noodlebiscuits");
    caze.setCaseType("HI");
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(caseRepository.findById((eq(123)))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setCaseRef("123");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(rabbitTemplate, never())
        .convertAndSend(any(), anyString(), any(FieldworkFollowup.class));
  }

  @Test
  public void testBlankFieldCoordinatorIgnored() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    CaseRepository caseRepository = mock(CaseRepository.class);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    FieldworkFollowupBuilder fieldworkFollowupBuilder = mock(FieldworkFollowupBuilder.class);

    UndeliveredMailReceiver underTest =
        new UndeliveredMailReceiver(
            rabbitTemplate, caseRepository, uacQidLinkRepository, fieldworkFollowupBuilder);

    Case caze = new Case();
    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();

    // Given
    when(caseRepository.findById((eq(123)))).thenReturn(Optional.of(caze));
    when(fieldworkFollowupBuilder.buildFieldworkFollowup(
            eq(caze), eq("dummy"), eq("dummy"), eq(true)))
        .thenReturn(fieldworkFollowup);

    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent.getPayload().getFulfilmentInformation().setCaseRef("123");

    // When
    underTest.receiveMessage(responseManagementEvent);

    // Then
    verify(rabbitTemplate, never())
        .convertAndSend(any(), anyString(), any(FieldworkFollowup.class));
  }
}
