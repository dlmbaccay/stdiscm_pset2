import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";

    private int maxInstances;
    private int tankPlayers;
    private int healerPlayers;
    private int dpsPlayers;
    private int minClearTime;
    private int maxClearTime;

    public Config(String configFilePath) {
        maxInstances = 3;
        tankPlayers = 5;
        healerPlayers = 5;
        dpsPlayers = 15;
        minClearTime = 5;
        maxClearTime = 15;
        
        Properties props = new Properties();
        try (FileInputStream file = new FileInputStream(configFilePath)) {
            props.load(file);
            try {
                maxInstances = Integer.parseInt(props.getProperty("n"));
                tankPlayers = Integer.parseInt(props.getProperty("t"));
                healerPlayers = Integer.parseInt(props.getProperty("h"));
                dpsPlayers = Integer.parseInt(props.getProperty("d"));
                minClearTime = Integer.parseInt(props.getProperty("t1"));
                maxClearTime = Integer.parseInt(props.getProperty("t2"));
            } catch (NumberFormatException e) {
                System.err.println(RED + "Error parsing config values: " + e.getMessage() + RESET);
                System.out.println("Using default configuration values");
            }
        } catch (IOException e) {
            System.err.println("Error loading config file: " + e.getMessage());
            System.out.println("Using default configuration values");
        }
    }

    public void validate() {
        if (maxInstances <= 0) {
            System.err.println(RED + "Error: Number of instances must be greater than 0, using default value of 3" + RESET);
            maxInstances = 3;
        }
        if (tankPlayers < 0) {
            System.err.println(RED + "Error: Number of tank players cannot be negative, using default value of 5" + RESET);
            tankPlayers = 5;
        }
        if (healerPlayers < 0) {
            System.err.println(RED + "Error: Number of healer players cannot be negative, using default value of 5" + RESET);
            healerPlayers = 5;
        }
        if (dpsPlayers < 0) {
            System.err.println(RED + "Error: Number of DPS players cannot be negative, using default value of 15" + RESET);
            dpsPlayers = 15;
        }
        if (minClearTime <= 0) {
            System.err.println(RED + "Error: Minimum clear time must be greater than 0, using default value of 5" + RESET);
            minClearTime = 5;
        }
        if (maxClearTime < minClearTime) {
            System.err.println(RED + "Error: Maximum clear time cannot be less than minimum clear time, using default values" + RESET);
            minClearTime = 5;
            maxClearTime = 15;
        }
        if (maxClearTime > 15) System.out.println(RED + "Warning: Maximum clear time exceeds recommended value of 15 seconds" + RESET);

        System.out.println(GREEN + "Config loaded" + RESET);
    }

    public int getMaxInstances() { return maxInstances; }

    public int getTankPlayers() { return tankPlayers; }

    public int getHealerPlayers() { return healerPlayers; }

    public int getDpsPlayers() { return dpsPlayers; }

    public int getMinClearTime() { return minClearTime; }

    public int getMaxClearTime() { return maxClearTime; }
}