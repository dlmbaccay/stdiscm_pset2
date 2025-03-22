import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class DungeonManager {

  public static final String RESET = "\u001B[0m";
  public static final String RED = "\u001B[31m";
  public static final String GREEN = "\u001B[32m";
  public static final String YELLOW = "\u001B[33m";
  public static final String BLUE = "\u001B[34m";
  public static final String PURPLE = "\u001B[35m";
  public static final String CYAN = "\u001B[36m";
  public static final String BOLD = "\u001B[1m";
  
  private final int maxInstances;
  private final int tankPlayers;
  private final int healerPlayers;
  private final int dpsPlayers;
  private final int minClearTime;
  private final int maxClearTime;

  private final List<DungeonInstance> instances = new ArrayList<>();
  private final List<Thread> instanceThreads = new ArrayList<>();

  private final Semaphore tanks;
  private final Semaphore healers;
  private final Semaphore dps;

  private final int maxPossibleParties;

  private final AtomicInteger expectedParties = new AtomicInteger(0);

  private final CountDownLatch allPartiesFormed;

  public DungeonManager(int maxInstances, int tankPlayers, int healerPlayers, int dpsPlayers, int minClearTime, int maxClearTime) {
    this.maxInstances = maxInstances;
    this.tankPlayers = tankPlayers;  
    this.healerPlayers = healerPlayers;
    this.dpsPlayers = dpsPlayers;
    this.minClearTime = minClearTime;
    this.maxClearTime = maxClearTime;

    tanks = new Semaphore(tankPlayers);
    healers = new Semaphore(healerPlayers);
    dps = new Semaphore(dpsPlayers);

    this.maxPossibleParties = Math.min(Math.min(tankPlayers, healerPlayers), dpsPlayers / 3);

    expectedParties.set(maxPossibleParties);

    allPartiesFormed = new CountDownLatch(maxPossibleParties);

    for (int i = 0; i < maxInstances; i++) {
      DungeonInstance instance = new DungeonInstance(i + 1, minClearTime, maxClearTime, this);
      instances.add(instance);
      Thread thread = new Thread(instance);
      instanceThreads.add(thread);
      thread.start();
    }

    printInit();
  }

  public void start() {
    Thread partyFormationThread = new Thread(this::formParties);
    partyFormationThread.start();

    try {
      allPartiesFormed.await();

      waitForAllDungeons();

      printSystemMessage("All possible parties served and dungeons cleared.");
      printFinalStatus();
      shutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void waitForAllDungeons() throws InterruptedException {
    boolean allDone = false;

    while (!allDone && !Thread.currentThread().isInterrupted()) {
      allDone = true;

      for (DungeonInstance instance : instances) {
        if (instance.isActive()) {
          allDone = false;
          break;
        }
      }
    }
  }

  private void formParties() {
    while (expectedParties.get() > 0) {
      try {
        if (tanks.availablePermits() > 0 && 
            healers.availablePermits() > 0 && 
            dps.availablePermits() >= 3) {

          boolean tanksAcquired = false;
          boolean healersAcquired = false;
          boolean dpsAcquired = false;

          tanksAcquired = tanks.tryAcquire(1);
          if (tanksAcquired) {
            healersAcquired = healers.tryAcquire(1);
            if (healersAcquired) {
              dpsAcquired = dps.tryAcquire(3);
              if (dpsAcquired) {
                assignPartyToInstance();
              }
            }
          }

          if (!(tanksAcquired && healersAcquired && dpsAcquired)) {
            if (tanksAcquired) tanks.release(1);
            if (healersAcquired) healers.release(1);
            if (dpsAcquired) dps.release(3);
          } 

          if (tanks.availablePermits() < 1 || 
              healers.availablePermits() < 1 || 
              dps.availablePermits() < 3) {

            int possibleParties = Math.min(Math.min(tanks.availablePermits(), healers.availablePermits()), dps.availablePermits() / 3);

            int originalExpectedParties = expectedParties.getAndSet(possibleParties);
            
            for (int i = 0; i < (originalExpectedParties - possibleParties); i++) allPartiesFormed.countDown();

            if (possibleParties == 0) break;
          }

          Thread.sleep(100);
        } else {
          int possibleParties = Math.min(Math.min(tanks.availablePermits(), healers.availablePermits()), dps.availablePermits() / 3);

          int countdownNeeded = expectedParties.getAndSet(possibleParties);

          for (int i = 0; i < countdownNeeded; i++) allPartiesFormed.countDown();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void assignPartyToInstance() throws InterruptedException {
    boolean partyAssigned = false;

    while (!partyAssigned) {
      for (DungeonInstance instance : instances) {
        synchronized (instance) {
          if (!instance.isActive()) {
            instance.enterDungeon();
            instance.notify();
            partyAssigned = true;
            expectedParties.decrementAndGet();
            allPartiesFormed.countDown();
            break;
          }
        }
      }

      if (!partyAssigned) {
        Thread.sleep(500);
      }
    }
  }

  private void shutdown() {
    // will interrupt all instance threads
    for (Thread thread : instanceThreads) {
      thread.interrupt();
    }
    
    // wait for all instance threads to terminate
    for (Thread thread : instanceThreads) {
      try {
        thread.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }    

    printSystemMessage("Dungeon LFG System terminated.\n");
  }

  // PRINTING FUNCTIONS

  private void printWithColor(boolean newLine, boolean isBold, String COLOR, String message) {
    if (newLine) System.out.println();
    if (isBold) System.out.println(BOLD + COLOR + message + RESET);
    else System.out.println(COLOR + message + RESET);
  }

  private void printInit() {
    printWithColor(true, true, YELLOW, "======================== [SYSTEM] Initialized ========================");
    printWithColor(true, true, YELLOW, "Dungeon LFG System initialized with:" + RESET);
    printWithColor(true, false, CYAN, " - " + maxInstances + " dungeon instances" + RESET);
    printWithColor(false, false, BLUE, " - " + tankPlayers + " tanks in queue" + RESET);
    printWithColor(false, false, GREEN, " - " + healerPlayers + " healers in queue" + RESET);
    printWithColor(false, false, RED, " - " + dpsPlayers + " DPS in queue" + RESET);
    printWithColor(false, false, PURPLE, " - " + "Dungeon clear time range: " + minClearTime + "-" + maxClearTime + " seconds" + RESET);
    printWithColor(false, false, "", " - " + "Maximum possible parties: " + maxPossibleParties + RESET);
  } 

  public synchronized void printRunningInstance(DungeonInstance instance) {
    printWithColor(true, true, CYAN, "------------------------ [RUNNING] Instance #" + instance.getId() + " -----------------------");
    printWithColor(true, true, "", "A party has been served and is currently clearing the dungeon...");
    printWithColor(true, true, "", "Players left in queue: ");
    printWithColor(false, false, BLUE, "Tanks: " + tanks.availablePermits() + "/" + tankPlayers + RESET);
    printWithColor(false, false, GREEN, "Healers: " + healers.availablePermits() + "/" + healerPlayers + RESET); 
    printWithColor(false, false, RED, "DPS: " + dps.availablePermits() + "/" + dpsPlayers + RESET);
    printWithColor(true, true, "", "Dungeon Instance Status:" + RESET);
    
    for (DungeonInstance di : instances) {
      System.out.println("Instance #" + di.getId() + ": " + (di == instance ? RED + "empty" + RESET + " -> " + GREEN + "active" + RESET : (di.isActive() ? GREEN + "active" + RESET : RED + "empty" + RESET)));
    }
  }

  public synchronized void printCompletedInstance(DungeonInstance instance) {
    printWithColor(true, true, GREEN, "------------------------ [COMPLETED] Instance #" + instance.getId() + " ---------------------");
    printWithColor(true, true, "", "Outgoing party cleared the dungeon in " + PURPLE + instance.getActualClearTime() + RESET + BOLD + " seconds" + RESET);

    printWithColor(true, true, "", "Dungeon Instance Status:" + RESET);
    for (DungeonInstance di : instances) {
      System.out.println("Instance #" + di.getId() + ": " + (di == instance ? GREEN + "active" + RESET + " -> " + RED + "empty" + RESET : (di.isActive() ? GREEN + "active" + RESET : RED + "empty" + RESET)));
    }
  }

  private void printFinalStatus() {
    printWithColor(true, true, YELLOW, "======================== [SYSTEM] LFG Summary ========================");
    printWithColor(false, true, "", "\nDungeon Instance Status:");

    for (DungeonInstance instance : instances) {
        System.out.println("Instance #" + instance.getId() + ": " + 
                          (instance.isActive() ? GREEN + "active" + RESET : RED + "empty" + RESET));
    }

    printWithColor(false, true, "", "\nDungeon Queue Summary:");
    for (DungeonInstance instance : instances) {
        System.out.println("Instance #" + instance.getId() + ": Served " + 
                        BOLD + CYAN + instance.getPartiesServed() + RESET + " parties, total time served: " + 
                        BOLD + PURPLE + instance.getTotalTimeServed() + RESET + " seconds");
    }
    
    printWithColor(true, true, "", "Players left in queue:");
    printWithColor(false, false, BLUE, "Remaining Tanks: " + tanks.availablePermits() + "/" + tankPlayers + RESET);
    printWithColor(false, false, GREEN, "Remaining Healers: " + healers.availablePermits() + "/" + healerPlayers + RESET);
    printWithColor(false, false, RED, "Remaining DPS: " + dps.availablePermits() + "/" + dpsPlayers + RESET);
  }

  private void printSystemMessage(String message) {
    printWithColor(true, true, YELLOW, "======================== [SYSTEM] Message ============================");
    printWithColor(true, true, "", message);
  }
}