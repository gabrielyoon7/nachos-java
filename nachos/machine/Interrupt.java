// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.*;

import java.util.TreeSet;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * The <tt>Interrupt</tt> class emulates low-level interrupt hardware. The
 * hardware provides a method (<tt>setStatus()</tt>) to enable or disable
 * interrupts.
 *
 * <p>
 * In order to emulate(모방하다) the hardware, we need to keep track of all pending
 * interrupts the hardware devices would cause, and when they are supposed to
 * occur.
 * 하드웨어가 발생시킨, 시킬 수 있는 대기 인터럽트를 추적한다. 
 * <p>
 * This module also keeps track of simulated time. Time advances only when the
 * following occur:
 * <ul>
 * <li>interrupts are enabled, when they were previously disabled
 * <li>a MIPS instruction is executed
 * </ul>
 * 인터럽트가 불가능 상태에서 발생가능한 상태로 전환된 경우, MIPS 명령어가 실행된 경우 시간의 흐름을 추적할 수 있다.
 * <p>
 * As a result, unlike real hardware, interrupts (including time-slice context
 * switches) cannot occur just anywhere in the code where interrupts are
 * enabled, but rather only at those places in the code where simulated time
 * advances (so that it becomes time for the hardware simulation to invoke an
 * interrupt handler).
 *
 * <p>
 * This means that incorrectly synchronized code may work fine on this hardware
 * simulation (even with randomized time slices), but it wouldn't work on real
 * hardware. But even though Nachos can't always detect when your program
 * would fail in real life, you should still write properly synchronized code.
 */
public final class Interrupt {
    /**
     * Allocate a new interrupt controller. 새로운 인터럽트 컨트롤러 할당
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    public Interrupt(Privilege privilege) {
		System.out.print(" interrupt");
		
		this.privilege = privilege; //privilege(kernel에서만 실행가능)
		privilege.interrupt = new InterruptPrivilege(); //인터럽트 프리빌리지 생성
		
		enabled = false;
		pending = new TreeSet<PendingInterrupt>();
    }

    /**
     * Enable interrupts. This method has the same effect as
     * <tt>setStatus(true)</tt>.
     */    
    public void enable() {
    	setStatus(true);
    }

    /**
     * Disable interrupts and return the old interrupt state. This method has
     * the same effect as <tt>setStatus(false)</tt>.
     *
     * @return	<tt>true</tt> if interrupts were enabled.
     */
    public boolean disable() {
    	return setStatus(false);
    }

    /**
     * Restore interrupts to the specified status. This method has the same
     * effect as <tt>setStatus(<i>status</i>)</tt>.
     *
     * @param	status	<tt>true</tt> to enable interrupts.
     */
    public void restore(boolean status) {
    	setStatus(status);
    }

    /**
     * Set the interrupt status to be enabled (<tt>true</tt>) or disabled
     * (<tt>false</tt>) and return the previous status. If the interrupt
     * status changes from disabled to enabled, the simulated time is advanced.
     *
     * @param	status		<tt>true</tt> to enable interrupts.
     * @return			<tt>true</tt> if interrupts were enabled.
     */
    public boolean setStatus(boolean status) {
		boolean oldStatus = enabled;
		enabled = status;
		
		if (oldStatus == false && status == true) {//인터럽트 가능 모드로 전환된 경우
		    //System.out.println("다시 인터럽트 커널모드 진입?");
		    tick(true); // 커널 모드로 전환?
		} 
		//else System.out.println("아니면 반복 종료?");
		return oldStatus; //enabled의 flag값 반환
    }

    /**
     * Tests whether interrupts are enabled.
     *
     * @return	<tt>true</tt> if interrupts are enabled.
     */
    public boolean enabled() {
    	return enabled; //인터럽트 가능 상태면 true 반환
    }

    /**
     * Tests whether interrupts are disabled.
     *
     * @return <tt>true</tt> if interrupts are disabled.
     */
    public boolean disabled() {
    	return !enabled; //인터럽트 불가능 상태면 true 반환
    }

    private void schedule(long when, String type, Runnable handler) { //대기 인터럽트 추가하는 메소드
		Lib.assertTrue(when>0);
		
		long time = privilege.stats.totalTicks + when;
		PendingInterrupt toOccur = new PendingInterrupt(time, type, handler);
	
		Lib.debug(dbgInt,
			  "Scheduling the " + type +
			  " interrupt handler at time = " + time);
	
		pending.add(toOccur);
    }

    private void tick(boolean inKernelMode) {
		Stats stats = privilege.stats;
	
		if (inKernelMode) { //커널 모드라면
		    stats.kernelTicks += Stats.KernelTick; // 커널 tick을 10만큼씩 증가(각 인터럽트가 활성화된 후 시뮬레이션 시간을 앞당길 양이 10, kernelTick)
		    stats.totalTicks += Stats.KernelTick;//총 tick 증가
		   // System.out.println("kernelTicks: "+stats.kernelTicks +", "+ stats.totalTicks);
		}
		else {
		    stats.userTicks += Stats.UserTick;
		    stats.totalTicks += Stats.UserTick;
		  // System.out.println("userTicks: "+stats.userTicks +", "+ stats.totalTicks);
		}
	
		if (Lib.test(dbgInt))
		    System.out.println("== Tick " + stats.totalTicks + " ==");
	
		enabled = false;
		checkIfDue();
		enabled = true;
		//System.out.println("다시 인터럽트 발생?");
    }

    private void checkIfDue() {
    	
		long time = privilege.stats.totalTicks;
	
		Lib.assertTrue(disabled());
	
		if (Lib.test(dbgInt))
		    print();
	
		if (pending.isEmpty()) //대기 인터럽트가 없으면 그냥 리턴
		    return;
	
		if (((PendingInterrupt) pending.first()).time > time) //실행할 준비가 된 대기 인터럽트가 없아도 그냥 리턴
		    return;
	
		Lib.debug(dbgInt, "Invoking interrupt handlers at time = " + time);
		
		while (!pending.isEmpty() &&
		       ((PendingInterrupt) pending.first()).time <= time) {
		    PendingInterrupt next = (PendingInterrupt) pending.first(); //대기 인터럽트가 존재하고 실행해야하는 시간이 지난 인터럽트가 있다면 트리에서 제일 첫번째 인터럽트 꺼내고
		    pending.remove(next); // 트리에서 제거
		    
		    Lib.assertTrue(next.time <= time);
	
		    if (privilege.processor != null)
		    	privilege.processor.flushPipe();
	
		    Lib.debug(dbgInt, "  " + next.type);
				
		    next.handler.run(); //조건을 모두 만족하면 핸들러 실행 - 타이머 클래스의 타이머 인터럽트나 autograder가 핸들러에용ㅋ
		    //System.out.println("Interrupt 클래스에서 대기 인터럽트 핸들러 실행");
		}
	
		Lib.debug(dbgInt, "  (end of list)");
    }

    private void print() { //출력 메시지 형식
		System.out.println("Time: " + privilege.stats.totalTicks
				   + ", interrupts " + (enabled ? "on" : "off"));
		System.out.println("Pending interrupts:");
	
		for (Iterator i=pending.iterator(); i.hasNext(); ) {
		    PendingInterrupt toOccur = (PendingInterrupt) i.next();
		    System.out.println("  " + toOccur.type +
				       ", scheduled at " + toOccur.time);
		}

		System.out.println("  (end of list)");
    }

    private class PendingInterrupt implements Comparable { 
    	PendingInterrupt(long time, String type, Runnable handler) { //대기 인터럽트 개체를 만드는 메소드
		    this.time = time;
		    this.type = type;
		    this.handler = handler;
		    this.id = numPendingInterruptsCreated++;
    	}

		public int compareTo(Object o) {
		    PendingInterrupt toOccur = (PendingInterrupt) o;
	
		    // can't return 0 for unequal objects, so check all fields
		    if (time < toOccur.time)
			return -1;
		    else if (time > toOccur.time)
			return 1;
		    else if (id < toOccur.id)
			return -1;
		    else if (id > toOccur.id)
			return 1;
		    else
			return 0;
		}

		long time;
		String type;
		Runnable handler;
	
		private long id;
    }
    
    private long numPendingInterruptsCreated = 0;

    private Privilege privilege;

    private boolean enabled;
    private TreeSet<PendingInterrupt> pending; //대기 인터럽트 리스트(트리): 발생, 객체에는 아직 전달X

    private static final char dbgInt = 'i';

    private class InterruptPrivilege implements Privilege.InterruptPrivilege {
    	
	public void schedule(long when, String type, Runnable handler) {
		System.out.println("");
	    Interrupt.this.schedule(when, type, handler);
	}

	public void tick(boolean inKernelMode) {
	    Interrupt.this.tick(inKernelMode);
	}
    }
}
