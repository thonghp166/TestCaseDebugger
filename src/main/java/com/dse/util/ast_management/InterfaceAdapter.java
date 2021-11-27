package com.dse.util.ast_management;

import com.dse.parser.object.IProjectNode;
import com.google.gson.*;

import java.lang.reflect.Type;

public class InterfaceAdapter<T>
        implements JsonSerializer<T>, JsonDeserializer<T> {

    @Override
    public final JsonElement serialize(final T object, final Type interfaceType, final JsonSerializationContext context)
    {
        if (object instanceof IProjectNode) {}
        JsonObject result = new JsonObject();
        result.add("type", new JsonPrimitive(object.getClass().getSimpleName()));
        result.add("properties", context.serialize(object, object.getClass()));
        return result;
    }

    @Override
    public final T deserialize(final JsonElement elem, final Type interfaceType, final JsonDeserializationContext context)
            throws JsonParseException
    {
        JsonObject jsonObject = elem.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        JsonElement element = jsonObject.get("properties");

        try {
            String thepackage = "com.dse.parser.object.";
            T re = context.deserialize(element, Class.forName(thepackage + type));
            return re;
        } catch (ClassNotFoundException cnfe) {
            throw new JsonParseException("Unknown element type: " + type, cnfe);
        }
    }

    private Type typeForName(final JsonElement typeElem)
    {
        try
        {
            return Class.forName(typeElem.getAsString());
        }
        catch (ClassNotFoundException e)
        {
            throw new JsonParseException(e);
        }
    }

    private JsonElement get(final JsonObject wrapper, final String memberName)
    {
        final JsonElement elem = wrapper.get(memberName);

        if (elem == null)
        {
            throw new JsonParseException(
                    "no '" + memberName + "' member found in json file.");
        }
        return elem;
    }

}
