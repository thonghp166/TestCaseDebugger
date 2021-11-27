package example.jackson;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

@JsonFilter("filter1")
public class Hello {

    private int x;

    private int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public static void main(String[] args) throws JsonProcessingException {
        Hello h = new Hello();
        h.setX(10);
        h.setY(1000);

        {
            FilterProvider filter1 = new SimpleFilterProvider().addFilter(
                    "filter1", SimpleBeanPropertyFilter.filterOutAllExcept("x"));

            ObjectMapper mapper = new ObjectMapper();

            String jsonData = mapper.writer(filter1)
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(h);
            System.out.println(jsonData);
        }

        {
            FilterProvider filter1 = new SimpleFilterProvider().addFilter(
                    "filter1", SimpleBeanPropertyFilter.filterOutAllExcept("y"));

            ObjectMapper mapper = new ObjectMapper();

            String jsonData = mapper.writer(filter1)
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(h);
            System.out.println(jsonData);
        }
    }
}