package example.gson.polynomic.example3;

import com.dse.parser.object.Node;
import com.google.gson.*;
import org.json.simple.JSONArray;

import java.lang.reflect.Type;

public class PersonSerializer implements JsonSerializer<Person> {

    public JsonElement serialize(Person src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonMerchant = new JsonObject();

        jsonMerchant.addProperty("name", src.getName());

        if (src instanceof MiddleAgePerson)
            jsonMerchant.addProperty("cmt", ((MiddleAgePerson) src).getCmt());

        JsonElement childrenArr = new JsonArray();
        for (Person child : src.getChildren())
            ((JsonArray) childrenArr).add(context.serialize(child, Person.class));

        jsonMerchant.add("children", childrenArr);

        return jsonMerchant;
    }
}