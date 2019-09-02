package uk.gov.ons.census.action.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.action.model.dto.UacCreateDTO;
import uk.gov.ons.census.action.model.dto.UacQidDTO;

@Component
public class CaseClient {
  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  public UacQidDTO getUacQid(UUID caseId, String questionnaireType) {
    String url = "http://" + host + ":" + port + "/uacqid/create/";
    RestTemplate restTemplate = new RestTemplate();
    UacCreateDTO caseDetails = new UacCreateDTO();
    caseDetails.setCaseId(caseId);
    caseDetails.setQuestionnaireType(questionnaireType);

    return restTemplate.postForObject(url, caseDetails, UacQidDTO.class);
  }
}
