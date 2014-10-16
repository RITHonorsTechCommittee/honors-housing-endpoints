package edu.rit.honors.housing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.devrel.samples.ttt.PMF;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

/**
 * Honors Housing Selection API
 *
 * Provides methods to get the rooms available for reservation and reserve one.
 */
@Api(
    name = "housing",
    version = "v1",
    scopes = {Constants.EMAIL_SCOPE},
    clientIds = {Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID},
    namespace = @ApiNamespace( ownerDomain = "honors.rit.edu",
        ownerName = "RIT Honors Program")
)
// Suppress warnings b/c all queries cause them >_-
@SuppressWarnings("unchecked")
public class Housing {

    /**
     * Get a list of the available rooms, organized into floors.
     * @throws NotFoundException if no rooms are found.
     */
    public FloorList rooms() throws NotFoundException {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(Floor.class);
        q.setOrdering("number asc");
        q.setRange(0,5);

        try {
            List<Floor> floors = (List<Floor>) q.execute();
            if(!floors.isEmpty()) {
                return new FloorList(this.addOccupants(floors));
            }
        } finally {
            q.closeAll();
        }

        // Default to 404
        throw new NotFoundException("no rooms available");
    }

    /**
     * Get the room that the logged in user has reserved.
     * 
     * @throws UnauthorizedException if no user is logged in or the currently
     * 		logged in user is not authorized to reserve rooms.
     * @throws NotFoundException if no reservation is found.
     */
    public Reservation current(User user) throws UnauthorizedException, NotFoundException {
    	this.authorize(user);
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(Reservation.class);
        q.setFilter("user == currentUser");
        q.declareParameters("User currentUser");

        try {
            Reservation r = (Reservation) q.execute(user);
            if( r != null ) {
                return r;
            }
        } finally {
            q.closeAll();
        }
        throw new NotFoundException("You have not reserved a room");
    }

    /**
     * Reserve a room for the logged in user
     * 
     * @param user The current user, filled automatically by the Endpoints API
     * @param room The room number to reserve.
     * @return the updated list of floors
     * @throws NotFoundException if no rooms are available or the room requested does not exist.
     * @throws UnauthorizedException if no one is logged in or the current user is not authorized to reserve rooms.
     * @throws ConflictException if the room is already full
     */
    @ApiMethod(httpMethod = "PUT")
    public FloorList reserve(User user, @Named("number") Integer room) throws NotFoundException, UnauthorizedException, ConflictException {
    	this.authorize(user);
        
        // Get complete list of rooms
        FloorList floors = this.rooms();
        
        // Check to make sure the user isn't re-reserving the same room
        Reservation current = this.current(user);
        if(current.getRoomNumber() == room){
        	// Nothing to do.
        	return floors;
        }
        
        // Check to make sure the room the user wants exists
        Room r = floors.getRoom(room);
        if(r == null){
        	throw new NotFoundException("Room does not exist");
        }
        
        // Ensure there is still space in the desired room
        if(r.getOccupants() == r.getCapacity()){
        	throw new ConflictException("Room is full");
        }
        

        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
	        // Unreserve the currently reserved room
	        if( current != null ) {
	            //TODO: make atomic with creating a new reservation.
	            pm.makeTransient(current);
	        }
	        
	        // Make a new reservation
	        Reservation newRes = new Reservation(user,room,new Date());
            pm.makePersistent(newRes);
            return floors;
            
        } finally {
            pm.close();
        }
    }

	/*
     * CRUD for rooms
     */
    
    /**
     * Get a room object by number.
     * 
     * This method does not check for reservations, so occupancy will be zero.
     * 
     * @param user The current user, filled automatically by the Endpoints SPI
     * @param room The room number
     * @return The Room object
     * @throws NotFoundException If the Room cannot be found in the datastore
     */
    @ApiMethod
    public Room getRoom(User user, @Named("number") Integer room) throws NotFoundException{
    	// make query.
    	// throw 404 if no room available
    	throw new NotFoundException("not implemented");
    }
    
    /**
     * Create a new Room object from the specified properties.  All parameters are required.
     * 
     * @param user The current user, filled automatically by the Endpoints SPI
     * @param num The room number
     * @param x The x position of the room (in pixels)
     * @param y The y position of the room (in pixels)
     * @param cap The room capacity
     * @return The Room created
     */
    @ApiMethod(httpMethod = "POST")
    public Room createRoom(User user, @Named("number") Integer num, 
    		@Named("x") Integer x, @Named("y") Integer y, @Named("capacity") Integer cap){
    	// search for existing room
    	
    	throw new UnsupportedOperationException("not implemented");
    }
    
    /**
     * Update an existing Room object, specified by the room number.  
     * Throws a 404 NotFoundException if the Room does not already exist.
     * 
     * @param user The current user, filled automatically by the Endpoints API
     * @param num The room number
     * @param x The x position of the room (in pixels)
     * @param y The y position of the room (in pixels)
     * @param cap The room capacity
     * @return The updated Room
     */
    @ApiMethod(httpMethod = "PUT")
    public Room updateRoom(User user, @Named("number") Integer number, @Nullable @Named("x") Integer x, @Nullable @Named("y") Integer y, @Nullable @Named("capacity") Integer cap){
    	// get other params from fields
    	throw new UnsupportedOperationException("not implemented");
    	
    }
    
    /**
     * Remove a Room from the datastore, specifed by the room number. 
     * Throws a 404 NotFoundException if the Room does not exist.
     * 
     * @param user
     * @param number
     * @return
     */
    @ApiMethod(httpMethod = "DELETE")
    public Room deleteRoom(User user, @Named("number") Integer number){
    	// get other params from fields
    	throw new UnsupportedOperationException("not implemented");
    }

    //TODO: API additions
    // - add room list
    // - add student list


    // -------------------------------------------------------- //
    // ------------------ END PUBLIC API ---------------------- //
    // -------------------------------------------------------- //

    // Look up reservations and add occupants to rooms
    private List<Floor> addOccupants(List<Floor> floors) {
        // Create temporary variables so that the parameters are not
        // modified, which could cause hard-to-diagnose serialization
        // problems.
        ArrayList<Room> tempRooms;
        ArrayList<Floor> retVal = new ArrayList<Floor>();
        Floor f2;
        Room r2;

        //TODO: only return rooms that are available this year.
        for( Floor f : floors) {
            tempRooms = new ArrayList<Room>();
            for( Room r : f.getRooms() ){
                r2 = new Room(r);
                List<Reservation> res = this.getReservationsForRoom(r2);
                r2.setOccupants(res.size());
                tempRooms.add(r2);
            }
            f2 = new Floor();
            f2.setNumber(f.getNumber());
            f2.setRooms(tempRooms);
            retVal.add(f2);
        }
        
        return retVal;
    }

    private List<Reservation> getReservationsForRoom(Room r) {
        // For every room, check to see how many reservations have been made.
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query q = pm.newQuery(Reservation.class);
        q.setFilter("roomNumber == room");
        q.declareParameters("Integer room");

        try {
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
    
    private void authorize(User user) throws UnauthorizedException {
    	if(user == null || !this.authorizedUser(user)){
    		throw new UnauthorizedException("You must be a current Honors Student");
    	}
	}

    private boolean authorizedUser(User user) {
		// TODO Auto-generated method stub
		return true;
	}

}
