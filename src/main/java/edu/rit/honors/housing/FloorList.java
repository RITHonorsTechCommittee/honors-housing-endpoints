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
		Room r = new Room();
        r.setNumber(room);
        int index;
        for(Floor f : this.getFloors()){
        	if((index = f.getRooms().indexOf(r)) > -1){
        		return f.getRooms().get(index);
        	}
        }
		return null;
	}

}
