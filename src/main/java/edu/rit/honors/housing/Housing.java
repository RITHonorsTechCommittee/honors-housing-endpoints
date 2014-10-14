package edu.rit.honors.housing;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.ServiceUnavailableException;
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
     */
    public List<Floor> rooms() throws NotFoundException, ServiceUnavailableException {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        Query q = pm.newQuery(Floor.class);
        q.setOrdering("number asc");
        q.setRange(0,5);

        try {
            List<Floor> floors = (List<Floor>) q.execute();
            if(!floors.isEmpty()) {
                return this.addOccupants(floors);
            }
        } finally {
            q.closeAll();
        }

        // Default to 404
        throw new NotFoundException("no rooms available");
    }

    /**
     * Get the currently reserved room for the logged in user
     */
    public Reservation current(User user) {
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
        return null;
    }

    /**
     * Reserve a room for the logged in user
     */
    @ApiMethod(httpMethod = "PUT")
    public void reserve(User user, @Named("number") Integer room) 
            throws ServiceUnavailableException, UnauthorizedException {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        //TODO: validate user is Honors student
        // pseudo code
        //   - make query
        //   - look up student list
        //   - check that email is in list

        Reservation current = this.current(user);
        if( current != null ) {
            //TODO: make atomic with creating a new reservation.
            pm.makeTransient(current);
        }

        //TODO: validate the room number. This must include checking
        //      that the current occupancy is less than the maximum.
        Reservation newRes = new Reservation(user,room,new Date());
        try {
            pm.makePersistent(newRes);
        } finally {
            pm.close();
        }
    }

    //TODO: API additions
    // - add/edit/remove rooms
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

}
