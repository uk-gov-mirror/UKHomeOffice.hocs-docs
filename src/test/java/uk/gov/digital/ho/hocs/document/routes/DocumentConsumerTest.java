package uk.gov.digital.ho.hocs.document.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.hocs.document.DocumentDataService;
import uk.gov.digital.ho.hocs.document.dto.camel.ProcessDocumentRequest;
import uk.gov.digital.ho.hocs.document.model.DocumentData;
import uk.gov.digital.ho.hocs.document.model.DocumentType;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class DocumentConsumerTest extends CamelTestSupport {

    private String endpoint = "direct://cs-dev-document-sqs";
    private String dlq = "mock:cs-dev-document-sqs-dlq";
    private String toEndpoint = "mock:malwarecheck";
    private UUID documentUUID = UUID.randomUUID();

    ObjectMapper mapper = new ObjectMapper();

    @Mock
    DocumentDataService documentDataService;

    private ProcessDocumentRequest request = new ProcessDocumentRequest(documentUUID.toString(), "someLink");

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
      return new DocumentConsumer(documentDataService, endpoint, dlq, 0,0,0,toEndpoint);
    }

    @Test
    public void shouldAddDocumentToMalwareQueueOnSuccess() throws Exception {
        UUID externalReferenceUUID = UUID.randomUUID();

        when(documentDataService.getDocumentData(any(String.class))).thenReturn(new DocumentData(externalReferenceUUID, DocumentType.ORIGINAL, "SomeDisplayName"));

        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldAddMessagetoDLQOnError() throws Exception {
        getMockEndpoint(dlq).expectedMessageCount(1);
        template.sendBody(endpoint, "BAD BODY");
        getMockEndpoint(dlq).assertIsSatisfied();
    }

    @Test
    public void shouldAddPropertiesToExchange() throws Exception {

        UUID externalReferenceUUID = UUID.randomUUID();

        when(documentDataService.getDocumentData(any(String.class))).thenReturn(new DocumentData(externalReferenceUUID, DocumentType.ORIGINAL, "SomeDisplayName"));
        MockEndpoint mockEndpoint = getMockEndpoint(toEndpoint);
        mockEndpoint.expectedPropertyReceived("externalReferenceUUID", externalReferenceUUID.toString());
        mockEndpoint.expectedPropertyReceived("uuid", documentUUID.toString());
        mockEndpoint.expectedMessageCount(1);
        template.sendBody(endpoint, mapper.writeValueAsString(request));
        mockEndpoint.assertIsSatisfied();
    }


}