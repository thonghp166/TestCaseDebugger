package example.gson.polynomic.example1;

import com.dse.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;

public class main {
    public static void main(String[] args) {
        // create a tree of object
        Person parent = new MiddleAgePerson();
        parent.setName("John");
        ((MiddleAgePerson) parent).setCmt("1234");

        Person child1 = new YoungPerson();
        child1.setName("John child 1");
        child1.setParent(parent);
        parent.getChildren().add(child1);

        Person child11 = new YoungPerson();
        child11.setName("John child 1-1");
        child11.setParent(child1);
        child1.getChildren().add(child11);

        Person child2 = new YoungPerson();
        child2.setName("John child 2");
        child2.setParent(parent);
        parent.getChildren().add(child2);

        // export the tree to json
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();

        RuntimeTypeAdapterFactory<Person> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Person.class)
                .registerSubtype(MiddleAgePerson.class)
                .registerSubtype(YoungPerson.class);

        Gson gson = builder.registerTypeAdapterFactory(shapeAdapterFactory)
                .setPrettyPrinting().create();

        String json = gson.toJson(parent, Person.class);
        Utils.writeContentToFile(json, "gson/john-family.json");

        // import json to create the original tree
        Person root = gson.fromJson(Utils.readFileContent(new File("gson/john-family.json")), Person.class);

        // fast checking
        System.out.println(root.getClass());
        for (Person child : root.getChildren())
            System.out.println("child: " + child.getClass());
    }
}
