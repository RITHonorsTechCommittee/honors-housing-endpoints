package edu.rit.honors.housing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ServiceUnavailableException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.devrel.samples.ttt.PMF;

import java.util.ArrayList;
import java.util.Arrays;
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
	
	// Internal constants
	private static final String STUDENT_PERMISSION = "Student list";
	private static final String EDIT_PERMISSION = "Editor list";
	private static final String ADMIN_PERMISSION = "Admin list";

	// Provide default administrators when none are set.
	// This array must be alphabetically sorted to function properly
	private static final String[] DEFAULT_ADMINS = { "gjd6793@g.rit.edu", "rdp2575@g.rit.edu" };


    /**
     * Get a list of the available rooms, organized into floors.
     * @throws NotFoundException if no rooms are found.
     */
	@ApiMethod(httpMethod = "GET")
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
	@ApiMethod(httpMethod = "GET")
    public Reservation current(User user) throws UnauthorizedException, NotFoundException {
    	this.authorize(user, STUDENT_PERMISSION);
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
    	this.authorize(user, STUDENT_PERMISSION);
        
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
     * @throws ServiceUnavailableException if an internal error occurs
     */
    @ApiMethod(httpMethod = "GET")
    public Room getRoom(User user, @Named("number") Integer room) 
    		throws NotFoundException, ServiceUnavailableException{
    	//TODO: authenticate user
    	// make query.
    	PersistenceManager pmf = PMF.get().getPersistenceManager();
    	
    	Query q = pmf.newQuery(Room.class);
    	q.setFilter("number = roomNumber");
    	q.declareParameters("roomNumber");
    	
    	try{
	    	Room r = (Room) q.execute(room);
	    	if( r != null){
	    		return r;
	    	}
    	}catch(ClassCastException cce){
    		throw new ServiceUnavailableException(cce);
    	}finally{
    		q.closeAll();
    	}
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
     * @throws ServiceUnavailableException if an internal error occurs
     * @throws ForbiddenException if the room already exists
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(httpMethod = "POST")
    public Room createRoom(User user, @Named("number") Integer num, 
    		@Named("x") Integer x, @Named("y") Integer y, @Named("capacity") Integer cap)
    				throws ServiceUnavailableException, ForbiddenException, UnauthorizedException{
    	this.authorize(user, EDIT_PERMISSION);
    	// search for existing room
    	try{
    		// this should throw a NotFoundException
    		this.getRoom(user, num);
    		throw new ForbiddenException("Room already exists");
    	}catch(NotFoundException nfe){
    		Room r = new Room();
    		r.setCapacity(cap);
    		r.setNumber(num);
    		r.setX(x);
    		r.setY(y);
    		PersistenceManager pm = PMF.get().getPersistenceManager();
    		return pm.makePersistent(r);
    	}
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
     * @throws ServiceUnavailableException if an internal error occurs
     * @throws ConflictException if the room does not exist 
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(httpMethod = "PUT")
    public Room updateRoom(User user, @Named("number") Integer number, 
    		@Nullable @Named("x") Integer x, @Nullable @Named("y") Integer y, 
    		@Nullable @Named("capacity") Integer cap) 
    				throws ServiceUnavailableException, ConflictException, UnauthorizedException{
    	this.authorize(user, EDIT_PERMISSION);
    	try{
    		PersistenceManager pm = PMF.get().getPersistenceManager();
    		// this should not throw a NotFoundException
    		Room r = this.getRoom(user, number);
    		if( null != x ){
    			r.setX(x);
    		}
    		if( null != y ){
    			r.setY(y);
    		}
    		if( null != cap){
    			r.setCapacity(cap);
    		}
    		return pm.makePersistent(r);
    	}catch(NotFoundException nfe){
    		throw new ConflictException("Room does not exist");
    	}
    }
    
    /**
     * Remove a Room from the datastore, specifed by the room number. 
     * Throws a 404 NotFoundException if the Room does not exist.
     * 
     * @param user
     * @param number
     * @return
     * @throws ServiceUnavailableException if an internal error occurs
     * @throws NotFoundException if the room does not exist
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(httpMethod = "DELETE")
    public Room deleteRoom(User user, @Named("number") Integer number) 
    		throws NotFoundException, ServiceUnavailableException, UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	Room r = this.getRoom(user, number);
    	pm.makeTransient(r);
    	return r;
    }
    
    /**
     * Gets the current list of available rooms
     * 
     * @param user the currently logged in user, filled in by Endpoints SPI
     * @return a list of room numbers as Strings
     * @throws NotFoundException if there is no list stored
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/room", httpMethod = "GET")
    public StringList getRoomList(User user) throws NotFoundException, UnauthorizedException{
    	this.authorize(user, EDIT_PERMISSION);
    	return this.getList("RoomList");
    }

    /**
     * Saves a new List of Strings containing the room numbers.  A list of Integers
     * might make more sense, but this way is easier internally.
     * 
     * @param user the currently logged-in User, filled in by the Endpoints SPI
     * @param numbers the List of Strings of room numbers
     * @return the saved list
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/room", httpMethod = "PUT")
    public StringList updateRoomList(User user, @Named("rooms") List<String> numbers) throws UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	return this.updateList(numbers, "RoomList");
    }
    
    /**
     * Gets the current list of authenticated student emails
     * 
     * @param user the currently logged in user, filled in by Endpoints SPI
     * @return a List of emails as Strings
     * @throws NotFoundException if there is no list stored
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(httpMethod = "GET")
    public StringList getStudentList(User user) 
    		throws NotFoundException, UnauthorizedException{
    	this.authorize(user, EDIT_PERMISSION);
    	return this.getList(STUDENT_PERMISSION);
    }

    /**
     * Saves a new List of Strings containing the authenticated student emails.
     * 
     * @param user the currently logged-in User, filled in by the Endpoints SPI
     * @param emails the List of emails
     * @return the saved list
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/student", httpMethod = "PUT")
    public StringList updateStudentList(User user, @Named("emails") List<String> emails) 
    		throws UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	return this.updateList(emails, STUDENT_PERMISSION);
    }
    
    /**
     * Gets the current list of authenticated editor emails
     * 
     * @param user the currently logged in user, filled in by Endpoints SPI
     * @return a list of room numbers as Strings
     * @throws NotFoundException if there is no list stored
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/editor", httpMethod = "GET")
    public StringList getEditorList(User user) throws NotFoundException, UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	return this.getList(EDIT_PERMISSION);
    }

    /**
     * Saves a new List of Strings containing the authenticated editor emails.
     * 
     * @param user the currently logged-in User, filled in by the Endpoints SPI
     * @param emails the List of emails
     * @return the saved list
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/editor", httpMethod = "PUT")
    public StringList updateEditorList(User user, @Named("emails") List<String> emails) 
    		throws UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	return this.updateList(emails, EDIT_PERMISSION);
    }
    
    /**
     * Gets the current list of authenticated admin emails
     * 
     * @param user the currently logged in user, filled in by Endpoints SPI
     * @return a list of room numbers as Strings
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/admin", httpMethod = "GET")
    public StringList getAdminList(User user) throws UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	try{
    		return this.getList(ADMIN_PERMISSION);
    	}catch(NotFoundException nfe){
    		// ensure there are always some admins
    		List<String> defaults = Arrays.asList(Housing.DEFAULT_ADMINS);
    		return new StringList(defaults);
    	}
    }

    /**
     * Saves a new List of Strings containing the authenticated admin emails.
     * 
     * @param user the currently logged-in User, filled in by the Endpoints SPI
     * @param emails the List of emails
     * @return the saved list
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/admin", httpMethod = "PUT")
    public StringList updateAdminList(User user, @Named("emails") List<String> emails) 
    		throws UnauthorizedException{
    	this.authorize(user, ADMIN_PERMISSION);
    	return this.updateList(emails, ADMIN_PERMISSION);
    }


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
    
    // gets a StringList from the datastore
    private StringList getList(String listName) throws NotFoundException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	
    	Query q = pm.newQuery(StringList.class);
    	q.setFilter("key == listName");
    	q.declareParameters("listName");
    	try {
    		return (StringList) q.execute(listName);
    	}catch(ClassCastException cce){
    		throw new NotFoundException(cce);
    	}
    }

    // stores a StringList in the datastore
    private StringList updateList(List<String> numbers, String listName){
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		StringList rooms = this.getList(listName);
    		rooms.setStrings(numbers);
    		return pm.makePersistent(rooms);
    	}catch(NotFoundException cce){
    		//there is no list with that key
    		StringList rooms = new StringList(numbers);
    		rooms.setKey(listName);
    		return pm.makePersistent(rooms);
    	}
    }
    
    private void authorize(User user, String permission) throws UnauthorizedException {
		boolean authorized = false;
    	if(user != null && user.getEmail() != null){
    		try {
    			StringList students = this.getList(permission);
    			authorized = students.getStrings().contains(user.getEmail());
				if( !authorized && permission == ADMIN_PERMISSION ){
					authorized = 0 <= Arrays.binarySearch(DEFAULT_ADMINS, user.getEmail());
				}
    		} catch (NotFoundException e) {
    			//TODO: log exception
    		}
    	}
		if( !authorized ){
			throw new UnauthorizedException("You must be on the "+permission+" to perform this action");
		}
	}

}
