package edu.rit.honors.housing;

import java.util.List;

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
    private List<String> string;
    
    public String getKey(){
    	return key;
    }
    
    public void setKey(String key){
    	this.key = key;
    }

    public List<String> getStrings(){
        return string;
    }

    public void setStrings(List<String> emails) {
        this.string = emails;
    }
    
    public StringList(){}
    
    public StringList(List<String> strings){ this.string = strings; }

}
