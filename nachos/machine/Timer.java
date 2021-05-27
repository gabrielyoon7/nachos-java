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
    public void setInterruptHandler(Runnable handler) { //인터럽트 핸들러 초기화
    	this.handler = handler;
    }

    /**
     * Get the current time.
     *
     * @return	the number of clock ticks since Nachos started.
     */
    public long getTime() {
    	return privilege.stats.totalTicks; //니초스 총 실행 tick(시간) 겟-★
    }

    private void timerInterrupt() { //인터럽트 서비스 루틴 실행 메소드
    	//System.out.println("Timer 클래스에서 timerInterrupt 실행");
		scheduleInterrupt(); //인터럽트 클래스의 스케쥴 메소드 호출
		scheduleAutoGraderInterrupt();
	
		lastTimerInterrupt = getTime();//현재 나초스 실행 시간(tick) 확인
	
		if (handler != null) //인터럽트 핸들러가(인터럽트 서비스 루틴이) 존재하면 - 알람 클래스의 타이머 인터럽트 메소드
		    handler.run(); //인터럽트 핸들러(인터럽트 서비스 루틴) 실행
    }

    private void scheduleInterrupt() { //스케쥴링에 인터럽트 정보 전달 메소드
    	//System.out.println("Timer 클래스에서 scheduleInterrupt 실행");
		int delay = Stats.TimerTicks; //지연시간(사용시간) 500으로 기본 설정되어있음
		delay += Lib.random(delay/10) - (delay/20); //지연시간(사용시간) 계산 - 무슨 계산법이지?

		privilege.interrupt.schedule(delay, "timer", timerInterrupt); //인터럽트 스케쥴링에 지연시간 정보 및 타이머 인터럽트 전달
    }

    private void scheduleAutoGraderInterrupt() {//스케쥴링에 autograder 인터럽트 정보 전달 메소드
    	//System.out.println("Timer 클래스에서 scheduleAutoGraderInterrupt 실행");
    	privilege.interrupt.schedule(1, "timerAG", autoGraderInterrupt); //autograder 인터럽트 스케쥴링에 지연시간 정보 및 타이머 인터럽트 전달
    }

    private long lastTimerInterrupt;
    private Runnable timerInterrupt;
    private Runnable autoGraderInterrupt;

    private Privilege privilege;
    private Runnable handler = null;
}

