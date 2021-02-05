import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.Attributes.Name;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

public class Updater {
	static final String usernamePassword = "admin:Login@123";
	static final String basicUrlAuthentication = "Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes());
	static final String cpApiUrl = "http://localhost:8080/openspecimen/rest/ng/collection-protocols";

	//Remove above hardcoding by taking inputs as command line param.
	
	public static void main(String[] args) throws IOException, CsvException {
		final String fileName = "input.csv";

		
		CSVReader reader = new CSVReader(new FileReader(fileName));
		reader.readNext();
		
		String[] columns = null;
		ObjectMapper objectMapper = new ObjectMapper();
		while((columns = reader.readNext()) != null) {
			String response = getCpByTitle(columns[0]);
			List<HashMap<String, Object>> cps = objectMapper.readValue(response, new TypeReference<List<HashMap<String, Object>>>(){});
			Map<String, Object> cp = cps.get(0);
			
			//Adding email of principal Investigator
			Map<String, Object> principalInvestigator = new HashMap<String, Object>();
			principalInvestigator.put("email", columns[1]);
			cp.put("principalInvestigator", principalInvestigator);
			
			//Adding sites to the CP
			List<Map<String, Object>> sites = new ArrayList<Map<String,Object>>();
			for(int i = 2; i < columns.length; i++) {
				Map<String, Object>site = new HashMap<String, Object>();
				site.put("name", columns[i]); //Sri: Not all CPs will have 3 sites. Check for not null before setting.
				sites.add(site);
			}
			
			cp.put("cpSites", sites);
			
			//System.out.println(cp.toString());
			String id = cp.get("id").toString();
			String payload = objectMapper.writeValueAsString(cp);
			
			System.out.println(payload); // Sri: Remove all print after debugging
			
			response = updateCP(payload,id);
			System.out.println(response);
			
		}		
		
	}
	
	public static String getCpByTitle(String title) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(cpApiUrl+ "?query=" + title.replace(" ", "+")).openConnection();
		connection.setRequestProperty("Authorization", basicUrlAuthentication);
		connection.setRequestMethod("GET");
		int responseCode = connection.getResponseCode();		
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String readLine = null;
		String response = "";
		
		while((readLine = br.readLine()) != null) {
			response = response + readLine;
		}
		
		return response;
	}
	
	public static String updateCP(String payload, String id) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(cpApiUrl + "/" + id).openConnection();
		connection.setRequestProperty("Authorization", basicUrlAuthentication);
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);
		OutputStream outputStream = connection.getOutputStream();
		outputStream.write(payload.getBytes());
		int responseCode = connection.getResponseCode();		
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String readLine = null;
		String response = "";
		
		while((readLine = br.readLine()) != null) {
			response = response + readLine;
		}
		
		return response;
	}
}
