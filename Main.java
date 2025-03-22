public class Main {
  public static void main(String[] args) {
    String configFile = "config.txt";
    if (args.length > 0) configFile = args[0];
    
    try {
        Config config = new Config(configFile);
        config.validate();
        
        DungeonManager dungeon = new DungeonManager(
            config.getMaxInstances(),
            config.getTankPlayers(),
            config.getHealerPlayers(),
            config.getDpsPlayers(),
            config.getMinClearTime(),
            config.getMaxClearTime()
        );
        
        dungeon.start();
    } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        System.exit(1);
    }
  }
}