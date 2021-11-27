package example.gson.polynomic.example3;

import com.google.gson.annotations.Expose;

public class MiddleAgePerson extends Person {
    @Expose
    private String cmt;

    public String getCmt() {
        return cmt;
    }

    public void setCmt(String cmt) {
        this.cmt = cmt;
    }
}
