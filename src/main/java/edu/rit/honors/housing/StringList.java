package edu.rit.honors.housing;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * A JDO PersistenceCapable list of strings
 */
@PersistenceCapable
public class StringList {
	
	@Persistent
	@PrimaryKey
	private String key;

    @Persistent
    private Set<String> string;
    
    public String getKey(){
    	return key;
    }
    
    public void setKey(String key){
    	this.key = key;
    }

    public Set<String> getStrings(){
        return string;
    }

    public void setStrings(Collection<String> strings) {
    	if(strings instanceof SortedSet<?>){
    		this.string = (Set<String>) strings;
    	} else {
    		this.string = new TreeSet<String>(strings);
    	}
    }
    
    public StringList(){}
    
    public StringList(Collection<String> strings){ this.setStrings(strings); }

}
