package uk.gov.ons.census.action.messaging;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionPlan;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ActionFulfilmentIT {

    @Value("${queueconfig.action-fulfilment-inbound-queue}")
    private String actionFulfilmentQueue;

    @Value("${queueconfig.events-exchange}")
    private String eventsExchange;

    @Value("${queueconfig.events-fulfilment-request-binding}")
    private String eventsFulfilmentRequestBinding;

    @Value("${queueconfig.outbound-printer-queue}")
    private String outboundPrinterQueue;

    @Autowired
    private RabbitQueueHelper rabbitQueueHelper;

    private EasyRandom easyRandom = new EasyRandom();

    @Before
    @Transactional
    public void setUp() {
        rabbitQueueHelper.purgeQueue(actionFulfilmentQueue);
        rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
    }

    @Test
    public void checkReceivedEventsAreEmitted() throws InterruptedException, IOException {
        BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

        // Given
        ResponseManagementEvent actionFulfilmentEvent = getResponseManagementEvent("1234");
        PrintFileDto printFileRequest = setupPrintFile();
        // WHEN
        rabbitQueueHelper.sendMessage(eventsExchange, eventsFulfilmentRequestBinding, actionFulfilmentEvent);

        PrintFileDto actualPrintFileDto = checkExpectedPrintFileDtoMessageReceived(outputQueue);

        assertThat(actualPrintFileDto.getCaseRef()).isEqualTo(printFileRequest.getCaseRef());


    }

    private PrintFileDto checkExpectedPrintFileDtoMessageReceived(BlockingQueue<String> queue)
            throws InterruptedException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String actualMessage = queue.poll(20, TimeUnit.SECONDS);
        assertNotNull("Did not receive message before timeout", actualMessage);

        return objectMapper.readValue(actualMessage, PrintFileDto.class);
    }

    private ResponseManagementEvent getResponseManagementEvent(String actionPlanId) {
        ResponseManagementEvent responseManagementEvent =
                easyRandom.nextObject(ResponseManagementEvent.class);

        responseManagementEvent
                .getPayload()
                .getCollectionCase()
                .setCaseRef(Integer.toString(easyRandom.nextInt()));
        responseManagementEvent.getPayload().getCollectionCase().setId(UUID.randomUUID().toString());
        responseManagementEvent.getPayload().getCollectionCase().setState("ACTIONABLE");
        responseManagementEvent.getPayload().getCollectionCase().setActionPlanId(actionPlanId);

        Random random = new Random();
        responseManagementEvent
                .getPayload()
                .getCollectionCase()
                .getAddress()
                .setLatitude(Double.toString(random.nextDouble()));
        responseManagementEvent
                .getPayload()
                .getCollectionCase()
                .getAddress()
                .setLongitude(Double.toString(random.nextDouble()));

        responseManagementEvent.getPayload().getCollectionCase().setRefusalReceived(false);

        return responseManagementEvent;
    }

    private PrintFileDto setupPrintFile() {
        return easyRandom.nextObject(PrintFileDto.class);
    }

    private ActionPlan setUpActionPlan(String name, String desc) {
        ActionPlan actionPlan = new ActionPlan();
        actionPlan.setName(name);
        actionPlan.setDescription(desc);
        actionPlan.setId(UUID.randomUUID());
        return actionPlan;
    }
}
