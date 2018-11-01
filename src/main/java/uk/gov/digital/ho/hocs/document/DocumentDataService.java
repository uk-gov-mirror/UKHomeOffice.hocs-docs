package uk.gov.digital.ho.hocs.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.document.aws.S3DocumentService;
import uk.gov.digital.ho.hocs.document.dto.camel.S3Document;
import uk.gov.digital.ho.hocs.document.exception.ApplicationExceptions;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentStatus;
import uk.gov.digital.ho.hocs.document.model.DocumentType;
import uk.gov.digital.ho.hocs.document.repository.DocumentRepository;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentDataService {

    private final DocumentRepository documentRepository;
    private final S3DocumentService s3DocumentService;

    @Autowired
    public DocumentDataService(DocumentRepository documentRepository, S3DocumentService s3DocumentService){
        this.documentRepository = documentRepository;
        this.s3DocumentService = s3DocumentService;
    }

    public DocumentData createDocument(UUID externalReferenceUUID, String displayName, DocumentType type) {
        log.debug("Creating Document: {}, external Reference  UUID: {}", displayName, externalReferenceUUID);
        DocumentData documentData = new DocumentData(externalReferenceUUID, type, displayName);
        documentRepository.save(documentData);
        log.info("Created Document: {}, external Reference UUID: {}", documentData.getUuid(), documentData.getExternalReferenceUUID());
        return documentData;
    }

    public void updateDocument(UUID documentUUID, DocumentStatus status, String fileLink, String pdfLink) {
        log.debug("Updating Document: {}", documentUUID);
        DocumentData documentData = getDocumentData(documentUUID);
        documentData.update(fileLink, pdfLink, status);
        documentRepository.save(documentData);
        log.info("Updated Document: {}", documentData.getUuid());
    }

    public DocumentData getDocumentData(String documentUUID) {
        return getDocumentData(UUID.fromString(documentUUID));
    }

    public DocumentData getDocumentData(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        if (documentData != null) {
            return documentData;
        } else {
            throw new ApplicationExceptions.EntityNotFoundException("Document UUID: %s not found!", documentUUID);
        }
    }

    public Set<DocumentData> getDocumentsByReference(UUID externalReferenceUUID) {
        Set<DocumentData> documents = documentRepository.findAllByExternalReferenceUUID(externalReferenceUUID);
        return documents;
    }

    public Set<DocumentData> getDocumentsByReferenceForType(UUID externalReferenceUUID, String type) {
        return documentRepository.findAllByExternalReferenceUUIDAndType(externalReferenceUUID,type);
    }

    public void deleteDocument(UUID documentUUID) {
        DocumentData documentData = documentRepository.findByUuid(documentUUID);
        documentData.setDeleted(true);
        documentRepository.save(documentData);
        log.info("Set Document to deleted: {}", documentUUID);
    }

    public S3Document getDocumentFile(UUID documentUUID) {
        DocumentData documentData = getDocumentData(documentUUID);
        try {
            return s3DocumentService.getFileFromTrustedS3(documentData.getFileLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage());
        }
    }

    public S3Document getDocumentPdf(UUID documentUUID) {
        DocumentData documentData = getDocumentData(documentUUID);
        try{
            return s3DocumentService.getFileFromTrustedS3(documentData.getPdfLink());
        } catch (IOException e) {
            throw new ApplicationExceptions.EntityNotFoundException(e.getMessage());
        }
    }
}