package example.gson.polynomic.example1;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public abstract class Person {
    //@Expose
    //private String type;

    @Expose
    private String name;

    private Person parent;

    @Expose
    private String parentName;

    @Expose
    private List<Person> children = new ArrayList<>();

    public Person() {
        //this.type = this.getClass().getSimpleName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getParent() {
        return parent;
    }

    public void setParent(Person parent) {
        this.setParentName(parent.getName());
        this.parent = parent;
    }

    public List<Person> getChildren() {
        return children;
    }

    public void setChildren(List<Person> children) {
        this.children = children;
    }

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }


}
