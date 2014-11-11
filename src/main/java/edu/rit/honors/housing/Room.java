package edu.rit.honors.housing;

import java.util.ArrayList;
import java.util.List;

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
    
    @Persistent
    private String shading = "";

    @NotPersistent
    private Integer occupants = 0;
    
    @NotPersistent
    private List<String> occupantNames = null;

    public Room() {}

    public Room(Room r) {
        this.number = r.number;
        this.capacity = r.capacity;
        this.x = r.x;
        this.y = r.y;
        this.occupants = r.occupants;
        this.shading = r.shading;
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
    
    public String getShading() {
    	return shading;
    }
    
    public void setShading(String shading) {
    	this.shading = shading;
    }

    public Integer getOccupants() {
        return occupants;
    }

    public void setOccupants(Integer occupants) {
        this.occupants = occupants;
    }
    
    public List<String> getOccupantNames() {
    	if(occupantNames != null && occupantNames.size() > 0) {
    		return occupantNames;
    	} else {
    		return null;
    	}
    }
    
    public void setOccupantNames(List<String> names) {
    	this.occupantNames = names;
    }
    
    public boolean addOccupant(String occupantName) {
    	if(this.occupantNames == null){
    		this.occupantNames = new ArrayList<String>(this.capacity);
    	}
    	if(this.occupantNames.add(occupantName)) {
    		this.occupants += 1;
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean deleteOccupant(String occupantName) {
    	if(this.occupantNames != null && this.occupantNames.remove(occupantName)) {
    		this.occupants -= 1;
    		return true;
    	} else {
    		return false;
    	}
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
     * @return true if the other Object is a Room and the values of the persistent fields match.
     */
    public boolean identical(Object other){
    	if(this.equals(other)){
    		Room r = (Room)other;
    		return this.x == r.x && this.y == r.y && this.capacity == r.capacity && this.shading == r.shading;
    	}else{
    		return false;
    	}
    }

}
