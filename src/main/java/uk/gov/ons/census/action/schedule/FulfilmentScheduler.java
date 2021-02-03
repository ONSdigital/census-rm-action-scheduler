package uk.gov.ons.census.action.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FulfilmentScheduler {
  private final FulfilmentProcessor fulfilmentProcessor;
  private static final Logger log = LoggerFactory.getLogger(FulfilmentScheduler.class);

  public FulfilmentScheduler(FulfilmentProcessor fulfilmentProcessor) {
    this.fulfilmentProcessor = fulfilmentProcessor;
  }

  @Scheduled(cron = "${fulfilment.batch.scheduled.time}")
  public void TriggerFulfilments() {
    try {
      fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();
    } catch (Exception e) {
      log.error("Unexpected exception while processing fulfilments", e);
      throw e;
    }
  }
}
