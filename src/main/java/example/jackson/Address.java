package example.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class Address {
    private Map<String, String> addressDetails = new HashMap<>();


    @JsonAnyGetter
    public Map<String, String> getAddress() {
        return addressDetails;
    }

    public static void main(String[] args) throws JsonProcessingException {
        Address address = new Address();
        Map<String, String> addressDetails = address.getAddress();
        addressDetails.put("village", "ABCD");
        addressDetails.put("district", "Varanasi");
        addressDetails.put("state", "Uttar Pradesh");
        addressDetails.put("country", "India");

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(address);
        System.out.println(jsonData);
    }
}