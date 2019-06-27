package uk.gov.ons.census.action.schedule;

import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;

import java.util.UUID;

@Component
public class PrintFileDtoBuilder {

    private final ActionInstructionBuilder actionInstructionBuilder;

    public PrintFileDtoBuilder(ActionInstructionBuilder actionInstructionBuilder) {
        this.actionInstructionBuilder = actionInstructionBuilder;
    }

    public PrintFileDto buildPrintFileDto(Case caze, ActionRule actionRule, long batchQty, UUID batchUUID) {
//        TODO: This function could be bust out into it's own component
        UacQidTuple uacQidTuple = actionInstructionBuilder.getUacQidLinks(caze);
        PrintFileDto printFileDto = new PrintFileDto();
        printFileDto.setIac(uacQidTuple.getUacQidLink().getUac());
        printFileDto.setQid(uacQidTuple.getUacQidLink().getQid());

        if (uacQidTuple.getUacQidLinkWales().isPresent()) {
            printFileDto.setIacWales(uacQidTuple.getUacQidLinkWales().get().getUac());
            printFileDto.setQidWales(uacQidTuple.getUacQidLinkWales().get().getQid());
        }

        printFileDto.setCaseRef(caze.getCaseRef());

        // TODO: where are these stored and used?
        printFileDto.setTitle("");
        printFileDto.setForename("");
        printFileDto.setSurname("");

        printFileDto.setAddressLine1(caze.getAddressLine1());
        printFileDto.setAddressLine2(caze.getAddressLine2());
        printFileDto.setAddressLine3(caze.getAddressLine3());
        printFileDto.setTownName(caze.getTownName());
        printFileDto.setPostcode(caze.getPostcode());
        printFileDto.setBatchId(batchUUID.toString());
        printFileDto.setBatchQty(batchQty);
        printFileDto.setActionType(actionRule.getActionType().toString());

        return printFileDto;
    }
}
