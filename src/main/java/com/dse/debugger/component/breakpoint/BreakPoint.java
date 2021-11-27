package com.dse.debugger.component.breakpoint;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import javafx.beans.property.SimpleBooleanProperty;

public class BreakPoint implements Comparable<BreakPoint> {
    @Expose
    @SerializedName("number")
    private int number;
    @Expose
    @SerializedName("file")
    private String file;
    @Expose
    @SerializedName("line")
    private int line;
    @Expose
    @SerializedName("fullname")
    private String full;
    @Expose
    @SerializedName("times")
    private int times;
    @Expose
    @SerializedName("disp")
    private String disp;
    @SerializedName("addr")
    @Expose
    private String addr;
    @Expose
    @SerializedName("func")
    private String func;
    @Expose
    @SerializedName("enabled")
    private String enabled;
    @Expose
    @SerializedName("cond")
    private String cond;

    private SimpleBooleanProperty selected = new SimpleBooleanProperty(true);

    public int getNumber() {
        return number;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public String getFull() {
        return full;
    }

    public int getTimes() {
        return times;
    }

    public String getDisp() {
        return disp;
    }

    public String getAddr() {
        return addr;
    }

    public String getFunc() {
        return func;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getCond() {
        return cond;
    }

    public void setCond(String cond) {
        this.cond = cond;
    }

    public boolean getSelected() {
        return selected.get();
    }

    public SimpleBooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selectedProperty) {
        this.selected.set(selectedProperty);
    }

    @Override
    public String toString() {
        return "BreakPoint{" +
                "number=" + number +
                ", file='" + file + '\'' +
                ", line=" + line +
                ", full='" + full + '\'' +
                ", times=" + times +
                ", disp='" + disp + '\'' +
                ", addr='" + addr + '\'' +
                ", func='" + func + '\'' +
                ", enabled='" + enabled + '\'' +
                ", cond='" + cond + '\'' +
                ", selected=" + selected +
                '}';
    }

    @Override
    public int compareTo(BreakPoint o) {
        if (this.number == o.number) {
            return 0;
        }
        if (this.number < o.number){
            return -1;
        }
        return 1;
    }

    public void update(BreakPoint breakPoint) {
//        this.number = breakPoint.number;
//        this.addr = breakPoint.addr;
//        this.cond = breakPoint.cond;
//        this.disp = breakPoint.disp;
//        this.file = breakPoint.file;
        this.times = breakPoint.times;
    }
}
