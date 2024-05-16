package util;

public abstract class ExtThread extends Thread {

    boolean threadStarted = false;
    boolean threadSuspended = false;
    boolean threadStopped = false;

    public final void run() {
      while (!threadStopped) {
        action();
      }
    }

    public synchronized final void startThread() {
      if(!threadStarted) start();
			threadStarted = true;
    }

    public synchronized final void stopThread() {
      threadStopped = true;
      threadSuspended = false;
      notify();
    }

    public synchronized final void suspendThread() {
      if(!threadStopped) threadSuspended = true;
    }

	public synchronized final void resumeThread() {
      if(!threadStopped) threadSuspended = false;
      notify();
    }

    public synchronized final void check() {
      try {
         while (threadSuspended && !threadStopped)
                 wait();
      } catch (InterruptedException e) {}
    }

    public boolean hasStarted() {
    	return threadStarted;
    }

    public boolean isSuspended() {
    	return threadSuspended;
    }

    public boolean hasStopped() {
    	return threadStopped;
    }


    public abstract void action();

}
