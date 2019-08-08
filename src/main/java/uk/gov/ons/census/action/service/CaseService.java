package uk.gov.ons.census.action.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.action.model.dto.CaseDetailsDTO;
import uk.gov.ons.census.action.model.dto.UacQidDTO;

@Service
public class CaseService {
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
