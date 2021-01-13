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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;


public class SampleClient2 implements IClientInterceptor {

    public static void main(String[] theArgs) throws IOException {
        StringBuilder allNames = new StringBuilder("");
        //Get file from resources folder
        ClassLoader classLoader = SampleClient2.class.getClassLoader();
        File file = new File(classLoader.getResource("SNames.txt").getFile());
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (allNames.length() < 1) {
                    allNames.append(line);
                } else {
                    allNames.append(" ").append(line);
                }
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(allNames);

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                //.where(Patient.FAMILY.matches().value("SMITH"))
                //  As I understant: I have to change it to:
                //  https://hapifhir.io/hapi-fhir/docs/client/generic_client.html
                .where(Patient.FAMILY.matches().values(allNames.toString()))
                .returnBundle(Bundle.class)
                .execute();
        List<Bundle.BundleEntryComponent> myEntries = response.getEntry();
        System.out.println("Size: " + myEntries.size());
    }


    // found one example:
    // https://github.com/hapifhir/hapi-fhir/blob/master/hapi-fhir-client/src/main/java/ca/uhn/fhir/rest/client/interceptor/LoggingInterceptor.java
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


    // can use it but currently don't
    //SampleClient2 sampleClient2 = new SampleClient2();
    //System.out.println(sampleClient2.getFile("SNames.txt"));
    private String getFile(String fileName) {
        StringBuilder result = new StringBuilder("");
        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }
            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
