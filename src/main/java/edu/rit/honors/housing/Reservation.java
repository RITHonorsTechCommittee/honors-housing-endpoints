package edu.rit.honors.housing;

import java.util.Date;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.IdGeneratorStrategy;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Reservation {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Persistent
    private Integer roomNumber;

    @Persistent
    private String user;
    
    @Persistent
    private String fullname;

    @Persistent
    private Date date;

    public Reservation(){}

    public Reservation(String user, Integer roomNumber, Date date) {
        this.user = user;
        this.roomNumber = roomNumber;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public Integer getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(Integer number) {
        this.roomNumber = number;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
