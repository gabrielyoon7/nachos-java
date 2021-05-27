// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.*;

/**
 * A hardware timer generates a CPU timer interrupt approximately every 500
 * clock ticks. This means that it can be used for implementing time-slicing,
 * or for having a thread go to sleep for a specific period of time.
 *
 * The <tt>Timer</tt> class emulates a hardware timer by scheduling a timer
 * interrupt to occur every time approximately 500 clock ticks pass. There is
 * a small degree of randomness here, so interrupts do not occur exactly every
 * 500 ticks.
 */
public final class Timer {
    /**
     * Allocate a new timer.
     *
     * @param	privilege      	encapsulates privileged access to the Nachos
     *				machine.
     */
    public Timer(Privilege privilege) {
		System.out.print(" timer\n");
		
		this.privilege = privilege;
		
		timerInterrupt = new Runnable() {
			public void run() { timerInterrupt(); }
		};
		
		autoGraderInterrupt = new Runnable() {
			public void run() {
			    Machine.autoGrader().timerInterrupt(Timer.this.privilege,
								lastTimerInterrupt);
			}
		};
	
		scheduleInterrupt();
    }

    /**
     * Set the callback to use as a timer interrupt handler. The timer
     * interrupt handler will be called approximately every 500 clock ticks.
     *
     * @param	handler		the timer interrupt handler.
     */
    public void setInterruptHandler(Runnable handler) { //���ͷ�Ʈ �ڵ鷯 �ʱ�ȭ
    	this.handler = handler;
    }

    /**
     * Get the current time.
     *
     * @return	the number of clock ticks since Nachos started.
     */
    public long getTime() {
    	return privilege.stats.totalTicks; //���ʽ� �� ���� tick(�ð�) ��-��
    }

    private void timerInterrupt() { //���ͷ�Ʈ ���� ��ƾ ���� �޼ҵ�
    	//System.out.println("Timer Ŭ�������� timerInterrupt ����");
		scheduleInterrupt(); //���ͷ�Ʈ Ŭ������ ������ �޼ҵ� ȣ��
		scheduleAutoGraderInterrupt();
	
		lastTimerInterrupt = getTime();//���� ���ʽ� ���� �ð�(tick) Ȯ��
	
		if (handler != null) //���ͷ�Ʈ �ڵ鷯��(���ͷ�Ʈ ���� ��ƾ��) �����ϸ� - �˶� Ŭ������ Ÿ�̸� ���ͷ�Ʈ �޼ҵ�
		    handler.run(); //���ͷ�Ʈ �ڵ鷯(���ͷ�Ʈ ���� ��ƾ) ����
    }

    private void scheduleInterrupt() { //�����층�� ���ͷ�Ʈ ���� ���� �޼ҵ�
    	//System.out.println("Timer Ŭ�������� scheduleInterrupt ����");
		int delay = Stats.TimerTicks; //�����ð�(���ð�) 500���� �⺻ �����Ǿ�����
		delay += Lib.random(delay/10) - (delay/20); //�����ð�(���ð�) ��� - ���� ��������?

		privilege.interrupt.schedule(delay, "timer", timerInterrupt); //���ͷ�Ʈ �����층�� �����ð� ���� �� Ÿ�̸� ���ͷ�Ʈ ����
    }

    private void scheduleAutoGraderInterrupt() {//�����층�� autograder ���ͷ�Ʈ ���� ���� �޼ҵ�
    	//System.out.println("Timer Ŭ�������� scheduleAutoGraderInterrupt ����");
    	privilege.interrupt.schedule(1, "timerAG", autoGraderInterrupt); //autograder ���ͷ�Ʈ �����층�� �����ð� ���� �� Ÿ�̸� ���ͷ�Ʈ ����
    }

    private long lastTimerInterrupt;
    private Runnable timerInterrupt;
    private Runnable autoGraderInterrupt;

    private Privilege privilege;
    private Runnable handler = null;
}

