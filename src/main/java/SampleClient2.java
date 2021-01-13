import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class SampleClient2 implements IClientInterceptor {
    // found one example:
    // https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-client/src/main/java/ca/uhn/fhir/rest/client/interceptor/LoggingInterceptor.java


    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                //  As I understant: I have to change it to:
                //      .where(Patient.FAMILY.matches().values("Smith", "Smyth".... 20 names from resource file.))
                .returnBundle(Bundle.class)
                .execute();

        List<Bundle.BundleEntryComponent> myEntries = response.getEntry();

        System.out.println(myEntries.size());

    }

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        // Not sure what for I should use it for.
        System.out.println("Client request: {}" + theRequest);
        try {
            String content = theRequest.getRequestBodyFromStream();
            if (content != null) {
                System.out.println("Client request body:\n{}" + content);
            }
        } catch (IllegalStateException | IOException e) {
            System.out.println("Failed to replay request contents (during logging attempt, actual FHIR call did not fail)" + e);
        }
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) throws IOException {
        // Not sure what for I should use it for.

        String message = "HTTP " + theResponse.getStatus() + " " + theResponse.getStatusInfo();
        String respLocation = "";

        /*
         * Add response location
         */
        List<String> locationHeaders = theResponse.getHeaders(Constants.HEADER_LOCATION);
        if (locationHeaders == null || locationHeaders.isEmpty()) {
            locationHeaders = theResponse.getHeaders(Constants.HEADER_CONTENT_LOCATION);
        }
        String timing = theResponse.getRequestStopWatch().toString();
        System.out.println("Client response: " + message + ", " + respLocation + ", in " + timing + " sec.");

        theResponse.bufferEntity();
        try (InputStream respEntity = theResponse.readEntity()) {
            if (respEntity != null) {
                final byte[] bytes;
                try {
                    bytes = IOUtils.toByteArray(respEntity);
                } catch (IllegalStateException e) {
                    throw new InternalErrorException(e);
                }
                System.out.println("Client response body:\n{}" + new String(bytes, StandardCharsets.UTF_8));
            } else {
                System.out.println("Client response body: (none)");
            }
        }
    }
}
