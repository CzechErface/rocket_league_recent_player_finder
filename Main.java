//Mainly coded by Czech / Luxy
//Attempts to find recent players by utilizing the rocket league log file.

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {
	public static class Player {
		public String name, steamId;
		public Player(String name, String steamId) {
			this.name = name;
			this.steamId = steamId;
		}
	}
	
	public static class PlayerFinder {
		private HashMap<String, Player> players;
		private String logFilePath;
		private JFileChooser fileChooser;
		
		public PlayerFinder() {
			players = new HashMap<String, Player>();
		}
		
		public void reloadLog() {
			//Make sure we have a log file path set
			if (logFilePath == null) {
				if (fileChooser == null) {
					fileChooser = new JFileChooser();
				    fileChooser.setFileFilter(new FileNameExtensionFilter("Rocket League LOG File", "log"));	
				}
				File logFile = new File(System.getProperty("user.home")+"/Documents/My Games/Rocket League/TAGame/Logs/Launch.log");
				if (!logFile.isFile()) {
					fileChooser.setCurrentDirectory(logFile);
					System.out.println("Unable to find the log file at the expected location.");
					fileChooser.showOpenDialog(null);
				    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
				    	logFilePath = fileChooser.getSelectedFile().getAbsolutePath();
				    else
				    	return; //best way to handle cancellation?
				} else
					logFilePath = logFile.getAbsolutePath();
			}
			//Read new players in from the log file
			HashMap<String, Player> newPlayers = new HashMap<String, Player>();
			File logFile = new File(logFilePath);
			BufferedReader reader = null;
			try {
			    reader = new BufferedReader(new FileReader(logFile));
			    String line = null;
			    while ((line = reader.readLine()) != null) {
			    	/* Only record player entries. An example line looks like this:
			    	 * [0048.28] PlayerNames: GFxData_PRI_TA_0 SanitizePlayerName PlayerName=Mr. Skeltal UniqueId=Steam-76561198063891760-0
			    	 */
			    	String namePrefix = "PlayerName=";
			    	String steamIdPrefix = "UniqueId=Steam-", steamIdSuffix = "-";
			    	//Find offsets for the name and steam id
			    	int nameOffset = line.indexOf(namePrefix);
			    	if (nameOffset == -1)
			    		continue;
			    	int idOffset = line.indexOf(steamIdPrefix, nameOffset);
			    	if (idOffset == -1)
			    		continue;
			    	int idStartOffset = idOffset + steamIdPrefix.length();
			    	int idEndOffset = line.indexOf(steamIdSuffix, idStartOffset);
			    	if (idEndOffset == -1)
			    		continue;
			    	//Grab the name and steam id and create an entry in our list
			    	String name = line.substring(nameOffset+namePrefix.length(), idOffset).trim();
			    	if (name.isEmpty())
			    		continue;
			    	String steamId = line.substring(idStartOffset, idEndOffset);
			    	if (steamId.isEmpty())
			    		continue;
			    	Player existingPlayer = newPlayers.get(name);
			    	if (existingPlayer == null) {
				    	newPlayers.put(name, new Player(name, steamId));
			    	} else if (!existingPlayer.steamId.equals(steamId)) {
			    		System.out.println("Warning: Skipping addition of conflicting steam ID with existing name: "+name+" -> "+steamId);
			    		continue;
			    	}
			    }
			} catch (IOException e) {
				System.out.println("The log file could not be read:");
				e.printStackTrace();
			    return;
			} finally {
			    try {
			        if (reader != null)
			            reader.close();
			    } catch (IOException e) {}
			}
			players = newPlayers;
			System.out.println("Log file at \""+logFilePath+"\" successfully loaded.");
		}
		
		public void findPlayer(String name) {
			System.out.println("Searching for player containing \""+name+"\":");
			name = name.toLowerCase();
			boolean foundSomeone = false;
			for (Player p : players.values()) {
				if (p.name.toLowerCase().indexOf(name) != -1) {
					foundSomeone = true;
					System.out.println("Found \"" + p.name + "\" with Steam ID \"" + p.steamId + "\"");
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(new URI("http://steamidfinder.com/?id="+p.steamId));
						} catch (IOException | URISyntaxException e) {
							System.out.println("Unable to open a web browser for some reason:");
							e.printStackTrace();
						}
					}
				}
			}
			if (!foundSomeone)
				System.out.println("No recent players found by that search query.");
		}
	}
	
	public static void main(String[] args) {
		//Memoized/constant strings
		final String help = "\nEnter '!' to reload the log file, 'q' to quit, or type part of a name to search.";
		String clear = "";
		while (clear.length() < 50) clear += '\n';
		//Start the finder and loop on input
		PlayerFinder pf = new PlayerFinder();
		System.out.println(clear);
		pf.reloadLog();
		System.out.println(help);
		Scanner scan = new Scanner(System.in);
		String inputLine;
		while (scan.hasNextLine()) {
			System.out.print(clear);
			inputLine = scan.nextLine();
			if (inputLine.equalsIgnoreCase("!"))
				pf.reloadLog();
			else if (inputLine.equalsIgnoreCase("q"))
				break;
			else
				pf.findPlayer(inputLine);
			System.out.println(help);
		}
		//We're done, say bye bye
		scan.close();
		System.out.println("Thank you for your interest in this little project, bye!");
	}
}
