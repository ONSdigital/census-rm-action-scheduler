package uk.gov.ons.census.action.poller;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.CaseToProcess;
import uk.gov.ons.census.action.model.repository.CaseToProcessRepository;

@Component
public class CaseToProcessPoller {
  private static final Logger log = LoggerFactory.getLogger(CaseToProcessPoller.class);

  private final CaseToProcessRepository caseToProcessRepository;
  private final CaseProcessor caseProcessor;

  @Value("${scheduler.chunksize}")
  private int chunkSize;

  public CaseToProcessPoller(
      CaseToProcessRepository caseToProcessRepository, CaseProcessor caseProcessor) {
    this.caseToProcessRepository = caseToProcessRepository;
    this.caseProcessor = caseProcessor;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  @Transactional
  public void processQueuedCases() {
    do {
      try (Stream<CaseToProcess> cases = caseToProcessRepository.findChunkToProcess(chunkSize)) {
        cases.forEach(this::process);
      }
    } while (caseToProcessRepository.count() > 0); // Don't go to sleep while there's work to do
  }

  private void process(CaseToProcess caseToProcess) {
    try {
      caseProcessor.process(caseToProcess);

      // Remove the case from the 'queue' if we didn't fail
      caseToProcessRepository.delete(caseToProcess);
    } catch (Exception e) {
      log.with(caseToProcess).error("Unexpected error. Poller will retry indefinitely", e);
      throw e; // This will cause the whole chunk to be rolled back
    }
  }
}
