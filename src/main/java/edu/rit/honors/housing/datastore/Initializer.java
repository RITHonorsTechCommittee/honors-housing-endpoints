package edu.rit.honors.housing.datastore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.datanucleus.store.query.AbstractQueryResult;

import com.google.api.server.spi.response.NotFoundException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import edu.rit.honors.housing.jdo.Floor;
import edu.rit.honors.housing.jdo.FloorList;
import edu.rit.honors.housing.jdo.InitializeToken;

public class Initializer {
	
	public static final String resourceFile = "/spec.json";
	
	public static void init(PersistenceManager pm) throws NotFoundException {
		Query q = pm.newQuery(InitializeToken.class);
		AbstractQueryResult r = (AbstractQueryResult) q.execute();
		if(r.size() > 0) {
			return;
		}
		
		Reader reader = null;
		InitializeToken token = new InitializeToken();
		try {
			// Save the token first to prevent concurrent adding of floors
			pm.makePersistent(token);
			pm.flush();
			
			Gson gson = new Gson();
			reader = new InputStreamReader(Initializer.class
					.getResourceAsStream("/spec.json"));
			FloorList flist = gson.fromJson(reader, FloorList.class);
			
			for( Floor floor : flist.getFloors() ) {
				pm.makePersistent(floor);
			}
		} catch( JsonIOException jse ) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to read spec.json", jse);
			pm.deletePersistent(token);
			throw new NotFoundException(jse);
		} catch( JsonSyntaxException jse ) {
			Logger.getGlobal().log(Level.SEVERE, "Failed to parse spec.json; invalid syntax", jse);
			pm.deletePersistent(token);
			throw new NotFoundException(jse);
		} finally {
			if( reader != null ) {
				try {
					reader.close();
				} catch (IOException e) {
					Logger.getGlobal().log(Level.WARNING, "Failed to close stream", e);
				}
			}
			pm.close();
		}
	}

}
