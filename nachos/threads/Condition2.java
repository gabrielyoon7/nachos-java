package nachos.threads;

import java.util.ArrayList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() { //Ű�� �ݳ��ϰ� wake() �߻� ������ sleep ���� ����
        Lib.assertTrue(conditionLock.isHeldByCurrentThread()); // ���� �����带

        conditionLock.release();

        boolean intStatus = Machine.interrupt().disable(); //���ͷ�Ʈ�� ���Ƶΰ�
        waitQueue.add(KThread.currentThread());//��� ť�� �޾Ƴ���
        KThread.currentThread().sleep();//sleep ���·� ����

        conditionLock.acquire();
        Machine.interrupt().restore(intStatus);//���ͷ�Ʈ �簳
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable(); //���ͷ�Ʈ�� ���Ƶΰ�
        if(!waitQueue.isEmpty()) {// ��� ť�� �����尡 ���������
            KThread thread = (KThread)waitQueue.get(0);// �ϳ� ������
            thread.ready();//ready ���·� �����
        }
        Machine.interrupt().restore(intStatus);//���ͷ�Ʈ �簳
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        while(!waitQueue.isEmpty()){//��� ť�� �����尡 ������������ ���� wake ��Ű��
            wake();
        }

    }

    // Place Condition2 testing code in the Condition2 class.

    // Example of the "interlock" pattern where two threads strictly
    // alternate their execution with each other using a condition
    // variable.

    private static class InterlockTest {
        private static Lock lock;
        private static Condition2 cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire(); // Ű�� ��
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();   // signal
                    cv.sleep();  // wait
                }
                lock.release(); // Ű �ݳ�
            }
        }

        public InterlockTest () {
            lock = new Lock();
            cv = new Condition2(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            // We need to wait for ping to finish, and the proper way
            // to do so is to join on ping.  (Note that, when ping is
            // done, pong is sleeping on the condition variable; if we
            // were also to join on pong, we would block forever.)
            // For this to work, join must be implemented.  If you
            // have not implemented join yet, then comment out the
            // call to join and instead uncomment the loop with
            // yields; the loop has the same effect, but is a kludgy
            // way to do it.
            ping.join();
            // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
        }
    }

    // Invoke Condition2.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
        new InterlockTest();
    }

    private Lock conditionLock;
    private ArrayList waitQueue = new ArrayList();
}
