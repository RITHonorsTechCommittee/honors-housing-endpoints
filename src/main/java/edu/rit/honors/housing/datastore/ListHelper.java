package edu.rit.honors.housing.datastore;

import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;

import com.google.api.server.spi.response.NotFoundException;

import edu.rit.honors.housing.jdo.StringList;

public class ListHelper {
	
	// gets a StringList from the datastore
    public static StringList getList(String listName, PersistenceManager pm) throws NotFoundException{
    	try{
    		return pm.getObjectById(StringList.class, listName);
    	}catch(JDOObjectNotFoundException jdoe){
    		throw new NotFoundException("List "+listName+" not found");
    	}
    }
    
    public static StringList updateList(String listName, PersistenceManager pm, List<String> str, boolean append){
    	try {
    		StringList rooms = getList(listName,pm);
    		if(append){
    			rooms.getStrings().addAll(str);
    		}else{
    			rooms.setStrings(str);
    		}
    		return pm.makePersistent(rooms);
    	}catch(NotFoundException cce){
    		//there is no list with that key
    		StringList rooms = new StringList(str);
    		rooms.setKey(listName);
    		return pm.makePersistent(rooms);
    	}
    }

}
