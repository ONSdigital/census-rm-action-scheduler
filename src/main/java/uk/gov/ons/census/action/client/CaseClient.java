package uk.gov.ons.census.action.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.action.model.dto.CaseDetailsDTO;
import uk.gov.ons.census.action.model.dto.UacQidDTO;

@Component
public class CaseClient {
  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  public UacQidDTO getUacQid(String questionnaireType) {
    String url = "http://" + host + ":" + port + "/uacqid/create/";
    RestTemplate restTemplate = new RestTemplate();
    CaseDetailsDTO caseDetails = new CaseDetailsDTO();
    caseDetails.setQuestionnaireType(questionnaireType);

    UacQidDTO uacQid = restTemplate.postForObject(url, caseDetails, UacQidDTO.class);
    return uacQid;
  }
}
