package edu.rit.honors.housing;

import java.util.ArrayList;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

/**
 * A JDO PersistenceCapable list of strings
 */
@PersistenceCapable
public class StudentList {

    @Persistent
    private ArrayList<String> students;

    public ArrayList<String> getStudents(){
        return students;
    }

    public void setStudents(ArrayList<String> emails) {
        this.students = emails;
    }

}
