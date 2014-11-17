package edu.rit.honors.housing.datastore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.datanucleus.store.query.AbstractQueryResult;

import com.google.api.server.spi.response.NotFoundException;

import edu.rit.honors.housing.Housing;
import edu.rit.honors.housing.jdo.Floor;
import edu.rit.honors.housing.jdo.FloorList;
import edu.rit.honors.housing.jdo.Reservation;
import edu.rit.honors.housing.jdo.Room;

public class RoomHelper {
	
	private PersistenceManager pm;
	public boolean checkAvailable = false;
	public boolean addOccupants = false;

	public Room getRoom(Integer number) {
		if(this.isAvailable(number)) {
			Query q = pm.newQuery(Room.class);
	    	q.setFilter("number == roomNumber");
	    	q.declareParameters("Integer roomNumber");
	    	
	    	try{
	    		AbstractQueryResult res = (AbstractQueryResult) q.execute(number);
	    		if( res.isEmpty() ){
	    	    	return null;
	    		}else{
	    			Room r = (Room) res.get(0);
	    			return this.addOccupants(r);
	    		}
	    	}finally{
	    		q.closeAll();
	    	}
		} else {
			return null;
		}
	}
	
	public Floor getFloor(String number) {
		Query q = pm.newQuery(Floor.class);
    	q.setFilter("number == floorNumber");
    	q.declareParameters("String floorNumber");
    	
    	try{
    		AbstractQueryResult res = (AbstractQueryResult) q.execute(number);
    		if( res.isEmpty() ){
    	    	return null;
    		}else{
    			if( res.size() > 1 ) {
    				Logger.getGlobal().warning("Floor "+number+" has a duplicate!");
    			}
    			Floor f = (Floor) res.get(0);
    			Logger.getGlobal().info("Floor "+f.getNumber()+" has "+f.getRooms().size()+" rooms");
    			// Need to detach a copy so that removing rooms from the floor
    			// doesn't remove the rooms from floors in the datastore.
    			f = pm.detachCopy(f);
    			Logger.getGlobal().info("Copy of Floor "+f.getNumber()+" has "+f.getRooms().size()+" rooms");
    			return this.processFloor(f);
    		}
    	}finally{
    		q.closeAll();
    	}
	}
	
	public FloorList getAllFloors() {
		Query q = pm.newQuery(Floor.class);
		q.setOrdering("number asc");
		AbstractQueryResult res = (AbstractQueryResult) q.execute();
		
		if( res.isEmpty() ){
	    	return null;
		}else{
			FloorList retVal = new FloorList(new ArrayList<Floor>());
			@SuppressWarnings("unchecked")
			Iterator<Floor> i = res.iterator();
			while(i.hasNext()){
				Floor f = i.next();
				Logger.getGlobal().info("Floor "+f.getNumber()+" has "+f.getRooms().size()+" rooms");
				Floor f2 = this.processFloor(pm.detachCopy(f));
				if( f2 != null ) {
					retVal.getFloors().add(f2);
				}
			}
			return retVal;
		}
	}
	
	public RoomHelper(PersistenceManager pm) {
		this.pm = pm;
	}
	
	public RoomHelper(PersistenceManager pm, boolean checkAvailable, boolean addOccupants) {
		this(pm);
		this.checkAvailable = checkAvailable;
		this.addOccupants = addOccupants;
	}
	
	private boolean isAvailable(Integer num) {
		if(num == null) {
			return false;
		} else if(this.checkAvailable) {
			try {
				return ListHelper.getList(Housing.ROOM_LIST, pm).getStrings().contains(num.toString());
			} catch (NotFoundException e) {
				return false;
			}
		} else {
			return true;
		}
	}
	
	private Room addOccupants(Room r) {
		if(this.addOccupants) {
			List<Reservation> res = this.getReservationsForRoom(r);
            List<String> names = new ArrayList<String>();
            for(Reservation name : res){
            	names.add(name.getFullname());
            }
            r.setOccupants(res.size());
            r.setOccupantNames(names);
            return r;
		} else {
			return r;
		}
	}
	
	private List<Reservation> getReservationsForRoom(Room r) {
        // For every room, check to see how many reservations have been made.
        Query q = pm.newQuery(Reservation.class);
        q.setFilter("roomNumber == room");
        q.declareParameters("Integer room");

        try {
            @SuppressWarnings("unchecked")
			List<Reservation> res = (List<Reservation>) q.execute(r.getNumber());
            if( res != null ) {
                return res;
            } else {
                return new ArrayList<Reservation>();
            }
        } finally {
            q.closeAll();
        }
    }
	
	private Floor processFloor(Floor f) {
		Logger.getGlobal().info("Floor "+f.getNumber()+" has "+f.getRooms().size()+" rooms.");
		Floor f2 = new Floor();
		f2.setNumber(f.getNumber());
		f2.setRooms(new ArrayList<Room>(f.getRooms().size()));
		for(Room r : f.getRooms()) {
			if(this.isAvailable(r.getNumber())) {
				f2.getRooms().add(this.addOccupants(r));
			}
		}
		// Floors with no rooms on them are hardly floors...
		if( f2.getRooms().size() > 0 ) {
			Logger.getGlobal().info("Floor "+f2.getNumber()+" has "+f2.getRooms().size()+" rooms, returning.");
			return f2;
		} else {
			Logger.getGlobal().info("Floor "+f2.getNumber()+" has no rooms, returning null.");
			return null;
		}
	}
	
}
