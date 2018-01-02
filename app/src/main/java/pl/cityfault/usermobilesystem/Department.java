package pl.cityfault.usermobilesystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Department {
    private int id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String departmentName) {
        this.name = departmentName;
    }

    public int getId() {
        return id;
    }

    public void setId(int departmentId) {
        this.id = departmentId;
    }
}
