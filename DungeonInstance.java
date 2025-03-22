import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DungeonInstance implements Runnable {
  private final int id;
  private final int minClearTime;
  private final int maxClearTime;
  private int actualClearTime;
  private volatile boolean isActive = false;
  private final AtomicInteger partiesServed = new AtomicInteger(0);
  private final AtomicInteger totalTimeServed = new AtomicInteger(0);
  private final Random random = new Random();
  private final DungeonManager dungeonManager;
  
  public DungeonInstance(int id, int minClearTime, int maxClearTime, DungeonManager dungeonManager) {
    this.id = id;
    this.minClearTime = minClearTime;
    this.maxClearTime = maxClearTime;
    this.dungeonManager = dungeonManager;
  }

  public synchronized void enterDungeon() {
    isActive = true;
  }

  @Override
  public void run() {
    while(!Thread.currentThread().isInterrupted()) {
      if (isActive) {
        try {
          actualClearTime = minClearTime + random.nextInt(maxClearTime - minClearTime + 1);
          dungeonManager.printRunningInstance(this);

          Thread.sleep(actualClearTime * 1000);

          synchronized (this) {
            isActive = false;
            partiesServed.incrementAndGet();
            totalTimeServed.addAndGet(actualClearTime);
            dungeonManager.printCompletedInstance(this);
            notify();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.out.println("Instance #" + id + " thread interrupted.");
          break;
        }
      } else {
        synchronized (this) {
          try {
            if (!isActive) {
              wait();
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }
  }

  public synchronized boolean isActive() { return isActive; }

  public int getPartiesServed() { return partiesServed.get(); }

  public int getTotalTimeServed() { return totalTimeServed.get(); }

  public int getActualClearTime() { return actualClearTime; }

  public int getId() { return id; }
}