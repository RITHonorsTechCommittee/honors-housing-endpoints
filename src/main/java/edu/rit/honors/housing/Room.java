package edu.rit.honors.housing;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;
import com.google.appengine.api.datastore.Key;

@PersistenceCapable
public class Room {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    public void setKey(Key key) {
        this.key = key;
    }

    @Persistent
    private Integer number;

    @Persistent
    private Integer capacity;

    @Persistent
    private Integer x;

    @Persistent
    private Integer y;

    @NotPersistent
    private Integer occupants = 0;

    public Room() {}

    public Room(Room r) {
        this.number = r.number;
        this.capacity = r.capacity;
        this.x = r.x;
        this.y = r.y;
        this.occupants = r.occupants;
    }
    
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
   
    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getOccupants() {
        return occupants;
    }

    public void setOccupants(Integer occupants) {
        this.occupants = occupants;
    }
    
    /**
     * Override Object.equals so that any two rooms with the same room number
     * are considered to be equal.  Use Room.identical if you want to compare
     * other properties of two Room objects.
     * 
     * @param other The Object with which to compare.
     * @return true if the other Object is a Room with the same room number
     */
    @Override
    public boolean equals(Object other){
    	return other instanceof Room && ((Room)other).number == this.number;
    }
    
    /**
     * Deeper comparison than Room.equals
     * 
     * @param other The Object with which to compare
     * @return true if the other Object is a Room with the same room number, x and y coordinates, and capacity
     */
    public boolean identical(Object other){
    	if(this.equals(other)){
    		Room r = (Room)other;
    		return this.x == r.x && this.y == r.y && this.capacity == r.capacity;
    	}else{
    		return false;
    	}
    }

}
