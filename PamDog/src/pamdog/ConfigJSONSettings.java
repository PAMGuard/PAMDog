package pamdog;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import pamdog.RestartInfo.RestartType;

/**
 * ConfigSettings implementation that reads/writes JSON files instead of using Java serialization
 * 
 * JSON is human readable and editable, making it easier to manage configuration files. 
 * 
 * @author Jamie Macaulay
 */
public class ConfigJSONSettings extends ConfigSettings {
	
	public ConfigJSONSettings( ) {
		System.out.println("Using JSON Config Settings");
	}
	
	
	@Override
	public boolean saveConfig(DogParams dogParams) {
		try {
			File conFile = getConfigFile();
			writeDogParams(conFile, dogParams);
			System.out.println("PamDog config saved to " + conFile.getAbsolutePath());
		} catch (Exception Ex) {
			System.out.println(Ex);
			return false;
		}
		
		return true;
	}

	@Override
	public DogParams loadConfig() {
		DogParams dogParams = null;
		File conFile = getConfigFile();
		if (conFile.exists() == false) {
			System.out.println("No PamDog config file");
			return null;
		}
		DogParams o = readDogParams(conFile);
		if (o != null && DogParams.class == o.getClass()) {
			dogParams = (DogParams) o;
		}
		return dogParams;
	}
	
	
	@Override
	protected String getSettingsFileName() {
		return "PamDogSettings.pjds";
	}
	
	/**
	 * Read a JSON file from a string. 
	 * @param file - the file. 
	 * @return the JSON string. 
	 */
	public static String readJSONString(File file) {

		try(FileReader fileReader = new FileReader(file)) {
			int ch; 
			StringBuilder jsonData = new StringBuilder(); 
			while((ch = fileReader.read()) != -1) {
				jsonData.append((char)ch); 
			}
			fileReader.close();
			return jsonData.toString(); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null; 
		}
	}

	/**
	 * Write a DogParams object to a JSON file. All fields are written.
	 * @param file destination file
	 * @param params DogParams instance to write
	 * @throws IOException
	 */
	public static void writeDogParams(File file, DogParams params) throws IOException {
		
		System.out.println("Writing DogParams to JSON file: " + file.getAbsolutePath());
		if (params == null) throw new IllegalArgumentException("params cannot be null");
		JSONObject jo = new JSONObject();
		jo.put("activeDog", params.isActiveDog());
		jo.put("workingFolder", params.getWorkingFolder());
		jo.put("javaFile", params.getJavaFile());
		jo.put("jre", params.getJre());
		jo.put("psfFile", params.getPsfFile());
		jo.put("libFolder", params.getLibFolder());
		jo.put("mxMemory", params.getMxMemory());
		jo.put("msMemory", params.getMsMemory());
		jo.put("udpPort", params.getUdpPort());
		jo.put("broadcastErrors", params.isBroadcastErrors());
		jo.put("udpPortErrors", params.getUdpPortErrors());
		jo.put("otherOptions", params.getOtherOptions());
		jo.put("otherVMOptions", params.getOtherVMOptions());
		jo.put("startWait", params.getStartWait());
		jo.put("allowSystemRestarts", params.isAllowSystemRestarts());
		jo.put("minRestartMinutes", params.getMinRestartMinutes());
		jo.put("lastRestartTime", params.getLastRestartTime());
		jo.put("deploy", params.isDeploy());
		if (params.getDeployDate() != null) jo.put("deployDate", params.getDeployDate());
		else jo.put("deployDate", JSONObject.NULL);

		// Restart info array
		jo.put("dogRestarts", new JSONArray());
		JSONArray ra = new JSONArray();
		ArrayList<RestartInfo> restarts = params.getDogRestarts();
		for (RestartInfo ri: restarts) {
			JSONObject rjo = new JSONObject();
			rjo.put("restartType", ri.getRestartType().name());
			rjo.put("restartTime", ri.getRestartTime());
			rjo.put("restartMessage", ri.getRestartMessage());
			ra.put(rjo);
		}
		jo.put("dogRestarts", ra);

		// ensure parent directory exists
		File parent = file.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		// write to file (overwrite)
		try (FileWriter fw = new FileWriter(file)) {
			fw.write(jo.toString(4)); // pretty print
			fw.flush();
		}
	}

	/**
	 * Read DogParams from a JSON file written by writeDogParams.
	 * Missing fields fall back to defaults in DogParams.
	 * @param file source file
	 * @return DogParams object or null if file cannot be read/parsed
	 */
	public static DogParams readDogParams(File file) {
		String s = readJSONString(file);
		if (s == null) return null;
		try {
			JSONObject jo = new JSONObject(s);
			DogParams params = new DogParams();
			if (jo.has("activeDog")) params.setActiveDog(jo.getBoolean("activeDog"));
			if (jo.has("workingFolder") && !jo.isNull("workingFolder")) params.setWorkingFolder(jo.getString("workingFolder"));
			if (jo.has("javaFile") && !jo.isNull("javaFile")) params.setJavaFile(jo.getString("javaFile"));
			if (jo.has("jre") && !jo.isNull("jre")) params.setJre(jo.getString("jre"));
			if (jo.has("psfFile") && !jo.isNull("psfFile")) params.setPsfFile(jo.getString("psfFile"));
			if (jo.has("libFolder") && !jo.isNull("libFolder")) params.setLibFolder(jo.getString("libFolder"));
			if (jo.has("mxMemory")) params.setMxMemory(jo.getInt("mxMemory"));
			if (jo.has("msMemory")) params.setMsMemory(jo.getInt("msMemory"));
			if (jo.has("udpPort")) params.setUdpPort(jo.getInt("udpPort"));
			if (jo.has("broadcastErrors")) params.setBroadcastErrors(jo.getBoolean("broadcastErrors"));
			if (jo.has("udpPortErrors")) params.setUdpPortErrors(jo.getInt("udpPortErrors"));
			if (jo.has("otherOptions") && !jo.isNull("otherOptions")) params.setOtherOptions(jo.getString("otherOptions"));
			if (jo.has("otherVMOptions") && !jo.isNull("otherVMOptions")) params.setOtherVMOptions(jo.getString("otherVMOptions"));
			if (jo.has("startWait")) params.setStartWait(jo.getInt("startWait"));
			if (jo.has("allowSystemRestarts")) params.setAllowSystemRestarts(jo.getBoolean("allowSystemRestarts"));
			if (jo.has("minRestartMinutes")) params.setMinRestartMinutes(jo.getInt("minRestartMinutes"));
			if (jo.has("lastRestartTime")) params.setLastRestartTime(jo.getLong("lastRestartTime"));
			if (jo.has("deploy")) params.setDeploy(jo.getBoolean("deploy"));
			if (jo.has("deployDate") && !jo.isNull("deployDate")) params.setDeployDate(jo.getLong("deployDate"));

			// Restart info
			if (jo.has("dogRestarts") && !jo.isNull("dogRestarts")) {
				JSONArray ra = jo.getJSONArray("dogRestarts");
				ArrayList<RestartInfo> restarts = new ArrayList<>();
				for (int i=0;i<ra.length();i++) {
					JSONObject rjo = ra.getJSONObject(i);
					String typeName = rjo.optString("restartType", "RESTARTRUN");
					RestartType rt = RestartType.RESTARTRUN;
					try { rt = RestartType.valueOf(typeName); } catch (IllegalArgumentException e) { /* keep default */ }
					long time = rjo.optLong("restartTime", System.currentTimeMillis());
					String msg = rjo.optString("restartMessage", "");
					restarts.add(new RestartInfo(rt, time, msg));
				}
				params.setDogRestarts(restarts);
			}

			return params;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static void main(String[] args) {
		try {
			DogParams params = new DogParams();
			params.setActiveDog(true);
			params.setJavaFile("/usr/bin/java-test");
			params.setLibFolder("lib6-test");
			params.setMxMemory(2048);
			params.setMsMemory(1024);
			params.setUdpPort(9000);
			params.setBroadcastErrors(true);
			params.setUdpPortErrors(9100);
			params.setOtherOptions("-test");
			params.setOtherVMOptions("-Xmx");
			params.setStartWait(5);
			params.setAllowSystemRestarts(true);
			params.setMinRestartMinutes(10);
			params.setLastRestartTime(System.currentTimeMillis());
			params.setDeploy(false);
			// add some restart info entries
			params.addRestart(new RestartInfo(RestartType.RESTARTPC, "PC rebooted for test"));
			params.addRestart(new RestartInfo(RestartType.RESTARTPAMGUARD, "PamGuard restart test"));

			File out = new File("target/dogparams_test.json");
			ConfigJSONSettings.writeDogParams(out, params);
			System.out.println("Wrote JSON to: " + out.getAbsolutePath());

			DogParams loaded = ConfigJSONSettings.readDogParams(out);
			if (loaded == null) {
				System.err.println("Failed to read DogParams from JSON");
				System.exit(2);
			}
			System.out.println("Loaded activeDog=" + loaded.isActiveDog());
			System.out.println("Loaded javaFile=" + loaded.getJavaFile());
			System.out.println("Loaded libFolder=" + loaded.getLibFolder());
			System.out.println("Loaded mxMemory=" + loaded.getMxMemory());
			System.out.println("Loaded udpPort=" + loaded.getUdpPort());
			System.out.println("Loaded broadcastErrors=" + loaded.isBroadcastErrors());
			System.out.println("Loaded dogRestarts count=" + loaded.getDogRestarts().size());
			if (!loaded.getDogRestarts().isEmpty()) {
				RestartInfo ri = loaded.getDogRestarts().get(0);
				System.out.println("First restart: type=" + ri.getRestartType() + ", time=" + ri.getRestartTime() + ", msg=" + ri.getRestartMessage());
			}
			System.out.println("Done.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}