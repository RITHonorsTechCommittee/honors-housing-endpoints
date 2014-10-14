package edu.rit.honors.housing;

/**
 * This class is a wrapper for Integer b/c Endpoints APIs
 * are not allowed to return primitive values.
 */
public class RoomNumber {

    private Integer number;

    public Integer getNumber(){
        return number;
    }

    public void setNumber(Integer num) {
        this.number = num;
    }

}
