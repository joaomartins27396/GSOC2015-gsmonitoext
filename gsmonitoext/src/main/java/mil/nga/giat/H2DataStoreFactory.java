package mil.nga.giat;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStore;

public class H2DataStoreFactory implements DataStoreFactorySpi {
	
	
	
	private final FileDataStore store = null;

	@Override
	public boolean canProcess(Map<String, Serializable> params) {
		boolean result = false;
		Object object;
		File file;
		if (params.containsKey("Path Name")) {
			object = params.get("Path Name");
			if (object instanceof File) {
				file = (File) object;
			} else {
				file = new File(object.toString());
			}
			if (file.exists() && file.isFile() && !file.isDirectory()) {
				result = true;
			}
		}
		return result;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Param[] getParametersInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<Key, ?> getImplementationHints() {
		return Collections.EMPTY_MAP;
	}

	@Override
	public DataStore createDataStore(Map<String, Serializable> params){
		return store;
	}

	@Override
	public DataStore createNewDataStore(Map<String, Serializable> arg0)
			throws IOException {
		//create Here the new Data store
		return null;
	}

}
