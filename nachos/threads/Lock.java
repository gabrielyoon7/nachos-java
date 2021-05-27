package nachos.threads;

import nachos.machine.*;

/**
 * A <tt>Lock</tt> is a synchronization primitive that has two states,
 * <i>busy</i> and <i>free</i>. There are only two operations allowed on a
 * lock:
 *
 * <ul>
 * <li><tt>acquire()</tt>: atomically wait until the lock is <i>free</i> and
 * then set it to <i>busy</i>.
 * <li><tt>release()</tt>: set the lock to be <i>free</i>, waking up one
 * waiting thread if possible.
 * </ul>
 *
 * <p>
 * Also, only the thread that acquired a lock may release it. As with
 * semaphores, the API does not allow you to read the lock state (because the
 * value could change immediately after you read it).
 */
public class Lock {
    /**
     * Allocate a new lock. The lock will initially be <i>free</i>.
     */
    public Lock() {
    }

    /**
     * Atomically acquire this lock. The current thread must not already hold
     * this lock.
     */
    public void acquire() {
	Lib.assertTrue(!isHeldByCurrentThread()); //현재 쓰레드가 아니라면 

	boolean intStatus = Machine.interrupt().disable(); //인터럽트를 막아두고
	KThread thread = KThread.currentThread(); //현재 쓰레드를 담아서

	if (lockHolder != null) { //Lock을 누구도 가지고 있지 않다면
	    waitQueue.waitForAccess(thread); // waitqueue에 접근 승인을 기다리게 달아놓고
	    KThread.sleep(); // 쓰레드를 재운다 - 어떤 쓰레드를 재우는 거지?
	}
	else {
	    waitQueue.acquire(thread); //Lock을 누군가 가지고 있다면
	    lockHolder = thread;//해당 쓰레드가 Lock을 갖도록 변경해준다.
	}

	Lib.assertTrue(lockHolder == thread);

	Machine.interrupt().restore(intStatus);
    }

    /**
     * Atomically release this lock, allowing other threads to acquire it.
     */
    public void release() {
	Lib.assertTrue(isHeldByCurrentThread());

	boolean intStatus = Machine.interrupt().disable();

	if ((lockHolder = waitQueue.nextThread()) != null) // Lock을 누군가 가지고 있다면
	    lockHolder.ready(); // 그 쓰레드를 ready 상태로 변경 
	
	Machine.interrupt().restore(intStatus);
    }

    /**
     * Test if the current thread holds this lock.
     *
     * @return	true if the current thread holds this lock.
     */
    public boolean isHeldByCurrentThread() { // Lockholder 쓰레드가 현재 쓰레드면 true 반환???
	return (lockHolder == KThread.currentThread());
    }

    private KThread lockHolder = null;
    private ThreadQueue waitQueue =
	ThreadedKernel.scheduler.newThreadQueue(true);
}
