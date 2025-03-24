import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Collections;


/**
 * AWS ECS Shell automate SSH login on ECS.
 * Require installation of AWS ECS tools.
 * 
 * javac Ecsh.java
 * java Ecsh
*/
public class Ecsh {
	
	private static String VERSION = "v1.3.1";
	
	private static String DEFAULT_PROFILE = "default";
	private static String DEFAULT_SHELL = "/bin/sh";
	
	private Scanner inputScanner = null;
	private Properties configurations = null;
	private String profile = null;
	private String cluster = null;

	
	/**
	 * Constructor
	 * 
	 * @param profile
	 */
	public Ecsh(String profile) {
		inputScanner = new Scanner(System.in);

		this.profile = profile;
		
		//load configuration only when profile is not null
		if( profile != null ) {
			loadConfigurations();
		}
	}

	
	/**
	 * Return the configuration file path under USER_HOME/.ecsh
	*/
	public String getConfigurationFilePath(){
		return System.getProperty("user.home") + System.getProperty("file.separator") + ".ecsh";
	}
	
	
	/**
	 * Load configurations
	*/
	public void loadConfigurations(){
		configurations = new Properties();
		try(InputStream is = new FileInputStream(getConfigurationFilePath())){
			configurations.load(is);

			cluster = configurations.getProperty(profile + ".cluster");
			
			System.out.println("Loading configuration file " + getConfigurationFilePath());
		} catch (IOException e) {
		}

		if( cluster == null ){
			System.out.println("No profile with name '" + profile + "' found. Exit.");

			System.exit(1);
		}
	}
	
	
	/**
	 * Store configurations
	*/
	public void storeConfigurations(String profile, String cluster){
		try(OutputStream os = new FileOutputStream(getConfigurationFilePath())){
			configurations.setProperty(profile + ".cluster", cluster);
			configurations.store(os, null);
			
			System.out.println("Saving Configuration file " + getConfigurationFilePath());
		} catch (IOException e) {
		} 
	}
	
	
	/**
	 * Is Windows?
	 * 
	 * @return true for windows, false otherwise (linux like)
	*/
	private boolean isWindows() throws Exception {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}
	
	
	/**
	 * Run OS command
	 * 
	 * @param command
	*/
	private String runCommand(String command) throws Exception {
		ProcessBuilder builder = new ProcessBuilder();

		if (isWindows()) {
			builder.command("cmd.exe", "/c", command);
		} else {
			builder.command("sh", "-c", command);
		}

		System.out.println("Running command:\n" + command);

		Process process = builder.start();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        StringBuilder commandOutput = new StringBuilder();

        while ((line = reader.readLine()) != null) {
			commandOutput.append(line).append("\n");
		}

		return commandOutput.toString();
	}
	
	
	/**
     * Parses a given string as Arn and return the resource
     *
     * @param arn - A string containing an ARN.
     */
    public String getArnResource(String arn) {
		try{
			int resourceColonIndex = arn.lastIndexOf('/');

			return arn.substring(resourceColonIndex + 1);
		}
		catch(Exception exc){
		}
		
		return null;
    }
	
	
	/**
	 * Read User choice
	 *
	 * @param message
	 * @param maxValue max number allowed
	*/
	public int readChoice(String message, int maxValue) throws Exception {
		int choice = 0;

		// Register a shutdown hook to clean up resources
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nInterruption detected. Exiting...");
			if (inputScanner != null) {
				inputScanner.close();
			}
		}));

		while(choice == 0) {
			System.out.print(message + " (1-" + maxValue + "): ");

			try {
				// Reading data using readLine
				String text = inputScanner.nextLine();

				choice = Integer.parseInt(text);

				if( choice > maxValue ){
					choice = 0;
				}
			}
			catch (NumberFormatException e) {
				System.err.println("Please enter a number between 1 and " + maxValue);
			} 
			catch (Exception e) {
				System.exit(1);
			}
		}
		
		return choice;
	}
	
	
	/**
	 * Read User text
	 * 
	 * @param message
	 * @param defaultValue
	 * @return
	 * @throws Exception
	 */
	public String readText(String message, String defaultValue) throws Exception {
		String text = null;
		
		// Register a shutdown hook to clean up resources
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nInterruption detected. Exiting...");
			if (inputScanner != null) {
				inputScanner.close();
			}
		}));

		while(text == null) {
			if( defaultValue != null ) {
				System.out.print(message + "[" + defaultValue + "]: ");
			}
			else {
				System.out.print(message + ": ");
			}

			try {
				// Reading data using readLine
				text = inputScanner.nextLine();

				if( "".equals(text)  ){
					if( defaultValue != null ) {
						text = defaultValue;
					}
					else {
						text = null;
					}
				}
			}
			catch(Exception e){
				System.exit(1);
			}
		}
		
		return text;
	}
	
	
	/**
	 * Handle help
	*/
	public void handleHelp() {
		System.out.println("Usage: ecsh [--options] [profile]");
		System.out.println("Where options are:");
		System.out.println("    --help:       This help");
		System.out.println("    --configure:  Configure wizard");
		System.out.println("");
		
		System.exit(0);
	}
	
	
	/**
	 * Handle Configure
	*/
	public void handleConfigure() throws Exception {
		String profile = readText("Profile name", DEFAULT_PROFILE);
		
		String cluster = readText("Cluster name", null);
		
		storeConfigurations(profile, cluster);
		
		System.exit(0);
	}
	
	
	/**
	 * Handle Job
	*/
	public void handle() throws Exception {
		System.out.println("Connect to Cluster: " + cluster);
		
		//Get list of Services
		String command = String.format("aws ecs list-services --output text --profile %s --cluster %s", profile, cluster);
		String cmdOutput = runCommand(command);
		String[] rawServices = cmdOutput.split("\n");
		
		List<String> services = new ArrayList<String>();
		for(String rawService: rawServices){
			String service = getArnResource(rawService);
			if( service != null && !"".equals(service) ){
				services.add(service);
			}
		}
		
		//sort services
		Collections.sort(services);
		
		//print service list
		for(int idx = 0; idx < services.size(); idx++){
			System.out.println( (idx + 1) + ") " + services.get(idx));
		}

		int choice = readChoice("Select Service", services.size());

		String serviceName = services.get(choice - 1);		
		
		//Get list of Tasks
		command = String.format("aws ecs list-tasks --output text --profile %s --cluster %s --service-name %s", profile, cluster, serviceName);
		cmdOutput = runCommand(command);
		String[] rawTasks = cmdOutput.split("\n");
		
		List<String> tasks = new ArrayList<String>();
		for(String rawTask: rawTasks){
			String task = getArnResource(rawTask);
			if( task != null && !"".equals(task) ){
				tasks.add(task);
				
				System.out.println(tasks.size() + ") " + task);
			}
		}
		
		if( tasks.isEmpty() ) {
			System.out.println("No task running, exit.");
			System.exit(0);
		}
		else if( tasks.size() > 1 ) {
			choice = readChoice("Select Task", tasks.size());
		}
		else{
			choice = 1;
		}

		String taskId = tasks.get(choice - 1);
		
		if(isWindows()) {
			command = String.format("start aws ecs execute-command --profile %s --cluster %s --task %s --interactive --command \"%s\"", profile, cluster, taskId, DEFAULT_SHELL);
		}
		else {
			command = String.format("xterm -e aws ecs execute-command --profile %s --cluster %s --task %s --interactive --command \"%s\"", profile, cluster, taskId, DEFAULT_SHELL);
		}
		
		runCommand(command);
		
		inputScanner.close();
	}
	
	
    /**
     * Main application entry point
     * 
     * @param args
     */
	public static void main(String[] args) throws Exception {
		System.out.println("AWS ECS Shell - " + VERSION);
		System.out.println("");

		//is there any args?
		String selectedProfile = null;
		if(args != null && args.length > 0) {
			if("--help".equals(args[0])) {
				Ecsh ecsh = new Ecsh(null);
				ecsh.handleHelp();
			}
			else if("--configure".equals(args[0])) {
				Ecsh ecsh = new Ecsh(null);
				ecsh.handleConfigure();
			}
			else {
				selectedProfile = args[0];
			}
		}
		else {
			selectedProfile = DEFAULT_PROFILE;
		}

		Ecsh ecsh = new Ecsh(selectedProfile);

		ecsh.handle();
	}
}