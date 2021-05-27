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
 * In order to emulate(����ϴ�) the hardware, we need to keep track of all pending
 * interrupts the hardware devices would cause, and when they are supposed to
 * occur.
 * �ϵ��� �߻���Ų, ��ų �� �ִ� ��� ���ͷ�Ʈ�� �����Ѵ�. 
 * <p>
 * This module also keeps track of simulated time. Time advances only when the
 * following occur:
 * <ul>
 * <li>interrupts are enabled, when they were previously disabled
 * <li>a MIPS instruction is executed
 * </ul>
 * ���ͷ�Ʈ�� �Ұ��� ���¿��� �߻������� ���·� ��ȯ�� ���, MIPS ��ɾ ����� ��� �ð��� �帧�� ������ �� �ִ�.
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
     * Allocate a new interrupt controller. ���ο� ���ͷ�Ʈ ��Ʈ�ѷ� �Ҵ�
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    public Interrupt(Privilege privilege) {
		System.out.print(" interrupt");
		
		this.privilege = privilege; //privilege(kernel������ ���డ��)
		privilege.interrupt = new InterruptPrivilege(); //���ͷ�Ʈ ���������� ����
		
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
		
		if (oldStatus == false && status == true) {//���ͷ�Ʈ ���� ���� ��ȯ�� ���
		    //System.out.println("�ٽ� ���ͷ�Ʈ Ŀ�θ�� ����?");
		    tick(true); // Ŀ�� ���� ��ȯ?
		} 
		//else System.out.println("�ƴϸ� �ݺ� ����?");
		return oldStatus; //enabled�� flag�� ��ȯ
    }

    /**
     * Tests whether interrupts are enabled.
     *
     * @return	<tt>true</tt> if interrupts are enabled.
     */
    public boolean enabled() {
    	return enabled; //���ͷ�Ʈ ���� ���¸� true ��ȯ
    }

    /**
     * Tests whether interrupts are disabled.
     *
     * @return <tt>true</tt> if interrupts are disabled.
     */
    public boolean disabled() {
    	return !enabled; //���ͷ�Ʈ �Ұ��� ���¸� true ��ȯ
    }

    private void schedule(long when, String type, Runnable handler) { //��� ���ͷ�Ʈ �߰��ϴ� �޼ҵ�
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
	
		if (inKernelMode) { //Ŀ�� �����
		    stats.kernelTicks += Stats.KernelTick; // Ŀ�� tick�� 10��ŭ�� ����(�� ���ͷ�Ʈ�� Ȱ��ȭ�� �� �ùķ��̼� �ð��� �մ�� ���� 10, kernelTick)
		    stats.totalTicks += Stats.KernelTick;//�� tick ����
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
		//System.out.println("�ٽ� ���ͷ�Ʈ �߻�?");
    }

    private void checkIfDue() {
    	
		long time = privilege.stats.totalTicks;
	
		Lib.assertTrue(disabled());
	
		if (Lib.test(dbgInt))
		    print();
	
		if (pending.isEmpty()) //��� ���ͷ�Ʈ�� ������ �׳� ����
		    return;
	
		if (((PendingInterrupt) pending.first()).time > time) //������ �غ� �� ��� ���ͷ�Ʈ�� ���Ƶ� �׳� ����
		    return;
	
		Lib.debug(dbgInt, "Invoking interrupt handlers at time = " + time);
		
		while (!pending.isEmpty() &&
		       ((PendingInterrupt) pending.first()).time <= time) {
		    PendingInterrupt next = (PendingInterrupt) pending.first(); //��� ���ͷ�Ʈ�� �����ϰ� �����ؾ��ϴ� �ð��� ���� ���ͷ�Ʈ�� �ִٸ� Ʈ������ ���� ù��° ���ͷ�Ʈ ������
		    pending.remove(next); // Ʈ������ ����
		    
		    Lib.assertTrue(next.time <= time);
	
		    if (privilege.processor != null)
		    	privilege.processor.flushPipe();
	
		    Lib.debug(dbgInt, "  " + next.type);
				
		    next.handler.run(); //������ ��� �����ϸ� �ڵ鷯 ���� - Ÿ�̸� Ŭ������ Ÿ�̸� ���ͷ�Ʈ�� autograder�� �ڵ鷯���뤻
		    //System.out.println("Interrupt Ŭ�������� ��� ���ͷ�Ʈ �ڵ鷯 ����");
		}
	
		Lib.debug(dbgInt, "  (end of list)");
    }

    private void print() { //��� �޽��� ����
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
    	PendingInterrupt(long time, String type, Runnable handler) { //��� ���ͷ�Ʈ ��ü�� ����� �޼ҵ�
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
    private TreeSet<PendingInterrupt> pending; //��� ���ͷ�Ʈ ����Ʈ(Ʈ��): �߻�, ��ü���� ���� ����X

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
