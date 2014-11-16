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
import edu.rit.honors.housing.datastore.Initializer;
import edu.rit.honors.housing.datastore.ListHelper;
import edu.rit.honors.housing.datastore.RoomHelper;
import edu.rit.honors.housing.jdo.Floor;
import edu.rit.honors.housing.jdo.FloorList;
import edu.rit.honors.housing.jdo.Reservation;
import edu.rit.honors.housing.jdo.Room;
import edu.rit.honors.housing.jdo.StringList;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.datanucleus.store.query.AbstractQueryResult;

/**
 * Honors Housing Selection API
 *
 * Provides methods to get the rooms available for reservation and reserve one.
 */
@Api(
    name = "housing",
    description = "Authenticates and stores data for Honors Housing room reservations",
    version = "v1",
    scopes = {Constants.EMAIL_SCOPE},
    clientIds = {Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID},
    namespace = @ApiNamespace( ownerDomain = "honors.rit.edu",
        ownerName = "RIT Honors Program")
)
public class Housing {
	
	// String list constants
	public static final String STUDENT_PERMISSION = "Student list";
	public static final String EDIT_PERMISSION = "Editor list";
	public static final String ADMIN_PERMISSION = "Admin list";
	public static final String ROOM_LIST = "Room list";


	// Provide default administrators when none are set.
	// This array must be alphabetically sorted to function properly
	private static final String[] DEFAULT_ADMINS = { "gjd6793@g.rit.edu", "rdp2575@g.rit.edu" };

    /**
     * Get a list of the available rooms, organized into floors.
     * 
     * @param user The currently logged in user, filled automatically by the Endpoints SPI
     * @throws NotFoundException if no rooms are found.
     * @throws UnauthorizedException if the user isn't on any list
     */
	@ApiMethod(httpMethod = "GET")
    public FloorList rooms(User user) throws NotFoundException, UnauthorizedException {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
        	this.authorize(user, pm, STUDENT_PERMISSION, EDIT_PERMISSION, ADMIN_PERMISSION);
        	
        	FloorList rooms = (new RoomHelper(pm,true,true)).getAllFloors();
        	if( rooms == null || rooms.getFloors().size() == 0 ) {
        		throw new NotFoundException("No rooms available.");
        	} else {
        		return rooms;
        	}
		} finally {
			pm.close();
		}
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
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
	    	this.authorize(user, pm, STUDENT_PERMISSION);
	
	        Reservation r = this.getReservation(user,pm);
	        if(null == r){
	        	throw new NotFoundException("You have not reserved a room");
	        }else{
	        	return r;
	        }
    	} finally {
    		pm.close();
    	}
    }

    /**
     * Reserve a room for the logged in user
     * 
     * @param user The current user, filled automatically by the Endpoints SPI
     * @param room The room number to reserve.
     * @return the updated list of floors
     * @throws NotFoundException if no rooms are available or the room requested does not exist.
     * @throws UnauthorizedException if no one is logged in or the current user is not authorized to reserve rooms.
     * @throws ConflictException if the room is already full
     */
	//TODO: make way to clear reservation
    @ApiMethod(httpMethod = "PUT")
    public FloorList reserve(User user, @Named("number") Integer room) throws NotFoundException, UnauthorizedException, ConflictException {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
        	this.authorize(user, pm, STUDENT_PERMISSION);
        	RoomHelper helper = new RoomHelper(pm,true,true);
        	
	        // Get complete list of rooms
	        FloorList floors = helper.getAllFloors();
	        
	        // Check to make sure the user isn't re-reserving the same room
	        Reservation current = this.getReservation(user, pm);
	        if(null != current && current.getRoomNumber() == room){
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
        
	        if( current != null ) {
	        	// change the room number of the reservation
	        	Integer oldRoom = current.getRoomNumber();
	            if(!floors.getRoom(oldRoom).deleteOccupant(user.getNickname())){
	            	Logger.getGlobal().warning("Could not delete old reservation for room "+oldRoom);
	            } else {
	            	Logger.getGlobal().info("Deleted old reservation for room "+oldRoom);
	            }
	            current.setRoomNumber(room);
	        } else {
		        // Make a new reservation
		        Reservation newRes = new Reservation(user.getEmail(),user.getNickname(),room,new Date());
	            pm.makePersistent(newRes);
	        }
	        
	        r.addOccupant(user.getNickname());
            return floors;
        } finally {
            pm.close();
        }
    }
    
    /**
     * Deletes the reservation of a user
     * 
     * @param user the currently logged in user, filled automatically by the Endpoints SPI
     * @return the floors with the reservation removed
     * @throws UnauthorizedException if the user is not on the student list
     * @throws NotFoundException if there are no rooms
     */
    @ApiMethod(httpMethod = "DELETE")
    public FloorList deleteReservation(User user) throws UnauthorizedException, NotFoundException {
    	PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
        	this.authorize(user, pm, STUDENT_PERMISSION);
        
	        // Get complete list of rooms
	        FloorList floors = (new RoomHelper(pm,true,true)).getAllFloors();
	        
	        // Check to make sure the user isn't re-reserving the same room
	        Reservation current = this.getReservation(user, pm);
	        if(null == current){
	        	// Nothing to do.
	        	return floors;
	        }
	        
	        Room r = floors.getRoom(current.getRoomNumber());
	        
	        r.setOccupants(r.getOccupants()-1);
	        r.getOccupantNames().remove(user.getNickname());
	        
	        pm.deletePersistent(current);
	        
            return floors;
        } finally {
            pm.close();
        }
    }
    
    /**
     * Loads room information from spec.json and saves it to the datastore,
     * if initialization has not already been performed.  Returns 204 No Content
     * on success (or already complete), 404 Not Found on failure.
     * 
     * @param user The currently logged-in user, filled automatically by the Endpoints SPI
     * @throws UnauthorizedException if the user is not an Admin
     * @throws NotFoundException if the operation is not successful.
     */
    @ApiMethod(httpMethod = "POST")
    public void initialize(User user) throws UnauthorizedException, NotFoundException {
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	authorize(user,pm,ADMIN_PERMISSION);
    	Initializer.init(pm);
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
     * @throws UnauthorizedException if the current user is not an editor or admin
     */
    @ApiMethod(httpMethod = "GET")
    public Room getRoom(User user, @Named("number") Integer room) 
    		throws NotFoundException, ServiceUnavailableException, UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try{
    		this.authorize(user, pm, EDIT_PERMISSION, ADMIN_PERMISSION);
    		return (new RoomHelper(pm)).getRoom(room);
    	}finally{
    		pm.close();
    	}
    }
    
    /**
     * Gets a single floor.  Like getRoom, does not filter by the room list
     * or compute occupants from reservations in the datastore.
     * 
     * @param user The current user, filled automatically by the Endpoints SPI
     * @param room The floor number
     * @return The first matched floor with the number requested, if it exists
     * @throws UnauthorizedException if the currently logged in user is not an Editor or Admin
     * @throws NotFoundException if no floor with that number exists
     */
    @ApiMethod(httpMethod = "GET")
    public Floor getFloor(User user, @Named("number") String number) 
    		throws UnauthorizedException, NotFoundException {
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	this.authorize(user, pm, Housing.EDIT_PERMISSION, Housing.ADMIN_PERMISSION);
    	return (new RoomHelper(pm)).getFloor(number);
    }
    
    /**
     * Create a new Room object from the specified properties.  All parameters are required.
     * 
     * @deprecated Rooms should be created using initialize
     * 
     * @param user The current user, filled automatically by the Endpoints SPI
     * @param num The room number
     * @param x The x position of the room (in pixels)
     * @param y The y position of the room (in pixels)
     * @param cap The room capacity
     * @param bgpath The SVG path to shade the room (optional)
     * @return The Room created
     * @throws ServiceUnavailableException if an internal error occurs
     * @throws ForbiddenException if the room already exists
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(httpMethod = "POST")
    @Deprecated
    public Room createRoom(User user, @Named("number") Integer num, @Named("floor") String floor,
    		@Named("x") Integer x, @Named("y") Integer y, @Named("capacity") Integer cap, 
    		@Nullable @Named("bgpath") String bgpath)
    				throws ServiceUnavailableException, ForbiddenException, UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try{
	    	this.authorize(user, pm, EDIT_PERMISSION, ADMIN_PERMISSION);
	    	// search for existing room
    		if((new RoomHelper(pm)).getRoom(num) != null) {
    			throw new ForbiddenException("Room "+num+" already exists");
    		} else {
	    		// create new room
	    		Room r = new Room();
	    		r.setCapacity(cap);
	    		r.setNumber(num);
	    		r.setX(x);
	    		r.setY(y);
	    		if(null != bgpath) {
	    			r.setBgpath(bgpath);
	    		}
	    		// find floor to which the room will be added
	    		Query q = pm.newQuery(Floor.class);
	    		q.setFilter("number == floorNumber");
	    		q.declareParameters("String floorNumber");
	    		
				AbstractQueryResult res = (AbstractQueryResult) q.execute(floor);
				if(res.isEmpty()){
	    			Floor f = new Floor();
	    			f.setNumber(floor);
	    			f.getRooms().add(r);
	    			pm.makePersistent(f);
				} else {
	    			Floor f = (Floor) res.get(0);
	    			f.getRooms().add(r);
				}
	    		
	    		return r;
    		}
    	}finally{
			pm.close();
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
     * @throws NotFoundException if the room does not exist 
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(httpMethod = "PUT")
    public Room updateRoom(User user, @Named("number") Integer number, 
    		@Nullable @Named("x") Integer x, @Nullable @Named("y") Integer y, 
    		@Nullable @Named("capacity") Integer cap, @Nullable @Named("bgpath") String bgpath) 
    				throws ServiceUnavailableException, NotFoundException, UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try{
    		this.authorize(user, pm, EDIT_PERMISSION, ADMIN_PERMISSION);
    		
    		// this should not throw a NotFoundException
    		Room r = (new RoomHelper(pm)).getRoom(number);
    		if( null != x ){
    			r.setX(x);
    		}
    		if( null != y ){
    			r.setY(y);
    		}
    		if( null != cap){
    			r.setCapacity(cap);
    		}
    		if(null != bgpath) {
    			r.setBgpath(bgpath);
    		}
    		return r;
    	} finally {
    		pm.close();
    	}
    }
    
    /**
     * Remove a Room from the datastore, specifed by the room number.
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
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try{
	    	this.authorize(user, pm, ADMIN_PERMISSION);
	    	Room r = (new RoomHelper(pm)).getRoom(number);
	    	Room retVal = new Room(r);
    		pm.deletePersistent(r);
        	return retVal;
    	}finally{
    		pm.close();
    	}
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
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		this.authorize(user, pm, EDIT_PERMISSION, ADMIN_PERMISSION);
    		return ListHelper.getList(Housing.ROOM_LIST, pm);
    	} finally {
    		pm.close();
    	}
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
    @ApiMethod(path="list/room", httpMethod = "POST")
    public StringList updateRoomList(User user, @Named("rooms") List<String> numbers,
    		@Nullable @Named("append") Boolean append) throws UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	this.authorize(user, pm, ADMIN_PERMISSION);
	    	append = (append == null)?Boolean.FALSE:append;
	    	return ListHelper.updateList(Housing.ROOM_LIST, pm, numbers, append.booleanValue());
    	} finally {
    		pm.close();
    	}
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
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	this.authorize(user, pm, EDIT_PERMISSION, ADMIN_PERMISSION);
	    	return ListHelper.getList(STUDENT_PERMISSION, pm);
    	} finally {
    		pm.close();
    	}
    }

    /**
     * Saves a new List of Strings containing the authenticated student emails.
     * 
     * @param user the currently logged-in User, filled in by the Endpoints SPI
     * @param emails the List of emails
     * @return the saved list
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/student", httpMethod = "POST")
    public StringList updateStudentList(User user, @Named("emails") List<String> emails,
    		@Nullable @Named("append") Boolean append) throws UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	this.authorize(user, pm, ADMIN_PERMISSION);
	    	append = (append == null)?Boolean.FALSE:append;
	    	return ListHelper.updateList(STUDENT_PERMISSION, pm, emails, append.booleanValue());
    	} finally {
    		pm.close();
    	}
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
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	this.authorize(user, pm, ADMIN_PERMISSION);
	    	return ListHelper.getList(EDIT_PERMISSION, pm);
    	} finally {
    		pm.close();
    	}
    }

    /**
     * Saves a new List of Strings containing the authenticated editor emails.
     * 
     * @param user the currently logged-in User, filled in by the Endpoints SPI
     * @param emails the List of emails
     * @return the saved list
     * @throws UnauthorizedException if user is not allowed to perform this action
     */
    @ApiMethod(path="list/editor", httpMethod = "POST")
    public StringList updateEditorList(User user, @Named("emails") List<String> emails,
    		@Nullable @Named("append") Boolean append) throws UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	this.authorize(user, pm, ADMIN_PERMISSION);
	    	append = (append == null)?Boolean.FALSE:append;
	    	return ListHelper.updateList(EDIT_PERMISSION, pm, emails, false);
    	} finally {
    		pm.close();
    	}
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
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
    		this.authorize(user, pm, ADMIN_PERMISSION);
    		return ListHelper.getList(ADMIN_PERMISSION, pm);
    	}catch(NotFoundException nfe){
    		// ensure there are always some admins
    		List<String> defaults = Arrays.asList(Housing.DEFAULT_ADMINS);
    		return new StringList(defaults);
    	} finally {
    		pm.close();
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
    @ApiMethod(path="list/admin", httpMethod = "POST")
    public StringList updateAdminList(User user, @Named("emails") List<String> emails,
    		@Nullable @Named("append") Boolean append) throws UnauthorizedException{
    	PersistenceManager pm = PMF.get().getPersistenceManager();
    	try {
	    	this.authorize(user, pm, ADMIN_PERMISSION);
	    	append = (append == null)?Boolean.FALSE:append;
	    	return ListHelper.updateList(ADMIN_PERMISSION, pm, emails, false);
    	} finally {
    		pm.close();
    	}
    }


    // -------------------------------------------------------- //
    // ------------------ END PUBLIC API ---------------------- //
    // -------------------------------------------------------- //

	// modularize the reservation query so it can be used by multiple API functions
	private Reservation getReservation(User user, PersistenceManager pm) {
		if(null == user){
			return null;
		}
		Query q = pm.newQuery(Reservation.class);
        q.setFilter("user == currentUser");
        q.declareParameters("String currentUser");

        try {
            AbstractQueryResult res = (AbstractQueryResult) q.execute(user.getEmail());
            if(res.size() == 0){
                return null;
            }else{
            	return (Reservation) res.get(0);
            }
        } finally {
            q.closeAll();
        }
	}
	
    private void authorize(User user, PersistenceManager pm, String... permission) throws UnauthorizedException {
		boolean authorized = false;
		String email = "";
    	if(user != null && user.getEmail() != null){
    		email = user.getEmail();
    		if(email.equals("example@example.com")) { // dev server
    			return;
    		}
    		for( String list : permission){
	    		try {
	    			StringList emails = ListHelper.getList(list,pm);
	    			authorized = (emails.getStrings() != null) && emails.getStrings().contains(email);
					if( !authorized && list.equals(ADMIN_PERMISSION) ){
						authorized = 0 <= Arrays.binarySearch(DEFAULT_ADMINS, email);
					}
					// break out of loop
					if(authorized) {
						return;
					}
	    		} catch (NotFoundException e) {
	    			Logger l = Logger.getLogger(this.getClass().getName());
	    			l.log(Level.INFO, "Could not authorize "+email+" list "+list+" not found.", e);
	    		}
	    	}
		}
    	//TODO: remove for production
    	//authorized = true;
		if( !authorized ){
			String lists = Arrays.deepToString(permission);
			throw new UnauthorizedException("You ("+email+") must be on one of "+lists+" to perform this action");
		}
	}

}
