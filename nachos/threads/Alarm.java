package nachos.threads;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
   Machine.timer().setInterruptHandler(new Runnable() {
      public void run() { timerInterrupt(); }
       });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() { //강제로 500틱때마다 강제로 뺏음
       System.out.println("Alarm 클래스에서 timerInterrupt 실행");
       //KThread.currentThread().yield();

      Iterator<KThread> it = waitingQueue.keySet().iterator();
      while (it.hasNext()) {
         KThread k = it.next();
         if (waitingQueue.get(k) <= Machine.timer().getTime()) {
            k.ready(); // 대기 시간이 지금이면 스레드 준비
            it.remove();
            // 스레드 1 개를 제거 했으므로 1로 돌아가기
         }
      }
      KThread.currentThread().yield();
   }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param   x   the minimum number of clock ticks to wait.
     *
     * @see   nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
   // for now, cheat just to get something working (busy waiting is bad)
       long wakeTime = Machine.timer().getTime() + x;
       
       if(x<=0)
          return;
   //while (wakeTime > Machine.timer().getTime()) {
   // KThread.yield();
      Machine.interrupt (). disable ();
      waitingQueue.put(KThread.currentThread(), wakeTime);
      KThread.sleep ();
     System.out.println ( "실제 wakeTime :"+ Machine.timer (). getTime ());
      Machine.interrupt (). enable ();
   }
    HashMap<KThread, Long> waitingQueue = new HashMap<>();
}

