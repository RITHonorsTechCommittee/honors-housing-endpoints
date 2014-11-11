package edu.rit.honors.housing;

import java.util.List;

public class FloorList {
	
	private List<Floor> floors;

	public List<Floor> getFloors() {
		return floors;
	}

	public void setFloors(List<Floor> floors) {
		this.floors = floors;
	}
	
	public FloorList(){}
	
	public FloorList(List<Floor> floors){
		this.floors = floors;
	}

	/**
	 * Find the room corresponding to the room number
	 * 
	 * @param room the room number to find
	 * @return the Room with that number or null if not found
	 */
	public Room getRoom(Integer room) {
        for(Floor f : this.getFloors()){
        	if(f.getRooms() != null){
	        	for(Room r : f.getRooms()) {
	        		if(r.getNumber() != null && r.getNumber().equals(room)){
	        			return r;
	        		}
	        	}
        	}
        }
		return null;
	}

}
