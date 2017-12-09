package pl.cityfault.usermobilesystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Created by Bartek on 2017-10-18.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Fault implements Serializable {
    private Long faultId;
    private String email;
    private String description;
    private Department department;
    private byte[] photo;
    private double latitude;
    private double longitude;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Long getFaultId() {
        return faultId;
    }
    public String getEmail() {
        return email;
    }

    public String getDescription() {
        return description;
    }

    public void setFaultId(Long id) {
        this.faultId = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
