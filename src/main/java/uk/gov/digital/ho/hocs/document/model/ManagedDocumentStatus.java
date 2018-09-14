package uk.gov.digital.ho.hocs.document.model;

import lombok.Getter;

public enum ManagedDocumentStatus {

    ACTIVE("Active"),
    EXPIRED("Expired");

    @Getter
    private String displayValue;

    ManagedDocumentStatus(String value) {
        displayValue = value;
    }
}
