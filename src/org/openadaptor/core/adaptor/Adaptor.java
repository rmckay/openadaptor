/*
 * [[
 * Copyright (C) 2001 - 2006 The Software Conservancy as Trustee. All rights
 * reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to
 * trademarks, copyrights, patents, trade secrets or any other intellectual
 * property of the licensor or any contributor except as expressly stated
 * herein. No patent license is granted separate from the Software, for
 * code that you delete from the Software, or for combinations of the
 * Software with other software or hardware.
 * ]]
 */

package org.openadaptor.core.adaptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.core.IMessageProcessor;
import org.openadaptor.core.IWriteConnector;
import org.openadaptor.core.Message;
import org.openadaptor.core.Response;
import org.openadaptor.core.jmx.Administrable;
import org.openadaptor.core.lifecycle.ILifecycleComponent;
import org.openadaptor.core.lifecycle.ILifecycleComponentContainer;
import org.openadaptor.core.lifecycle.ILifecycleComponentManager;
import org.openadaptor.core.lifecycle.ILifecycleListener;
import org.openadaptor.core.lifecycle.IRunnable;
import org.openadaptor.core.lifecycle.State;
import org.openadaptor.core.node.ReadNode;
import org.openadaptor.core.node.WriteNode;
import org.openadaptor.core.router.Pipeline;
import org.openadaptor.core.transaction.ITransactionInitiator;
import org.openadaptor.core.transaction.ITransactionManager;
import org.openadaptor.core.transaction.TransactionManager;
import org.openadaptor.util.Application;

public class Adaptor extends Application implements IMessageProcessor, ILifecycleComponentManager, Runnable,
    Administrable, ILifecycleListener {

  private static final Log log = LogFactory.getLog(Adaptor.class);

  /**
   * IMessageProcessor that this adaptor delegate message processing to
   * typically a Router
   */
  private IMessageProcessor processor;

  /**
   * ordered list of adaptor runnables
   */
  private List runnables;

  /**
   * ordered list of all the lifecycle components that it manages
   * this includes adaptor runnables too
   */
  private List components;

  /**
   * control whether adaptor creates a new thread to run the runnables
   * if false can only work for an adaptor with a single runnable
   */
  private boolean runRunnablesInCallingThread = false;

  /**
   * threads uses to run the runnables
   */
  private Thread[] runnableThreads = new Thread[0];

  /**
   * current state of the adaptor
   */
  private State state = State.STOPPED;

  /**
   * transaction manager, this is passed to the adaptor inpopints
   */
  private ITransactionManager transactionManager;

  /**
   * exit code, this is an aggregation of the adaptor runnable exit codes
   * 0 denotes that adaptor exited naturally
   */
  private int exitCode = 0;

  /**
   * TODO: remove this
   */
  private IMessageProcessor exceptionProcessor;
  
  /**
   * controls adaptor retry and start, stop, restart functionality
   */
  private AdaptorRunConfiguration runConfiguration;

  /**
   * shutdown hook
   */
  private Thread shutdownHook = new ShutdownHook();
  
  public Adaptor() {
    super();
    transactionManager = new TransactionManager();
    runnables = new ArrayList();
    components = new ArrayList();
  }

  public void setMessageProcessor(final IMessageProcessor processor) {
    if (this.processor != null) {
      throw new RuntimeException("message processor has already been set");
    }
    this.processor = processor;
  }

  private void registerComponents() {
    if (processor != null && processor instanceof ILifecycleComponentContainer) {
      log.debug("MessageProcessor is also a component container. Registering with processor.");
      runnables.clear();
      components.clear();
      ((ILifecycleComponentContainer) processor).setComponentManager(this);
    }
  }

  public void setRunInCallingThread(final boolean runRunnablesInCallingThread) {
    this.runRunnablesInCallingThread = runRunnablesInCallingThread;
  }

  public void setTransactionManager(final ITransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  public void setRunConfiguration(AdaptorRunConfiguration config) {
    runConfiguration = config;
  }
  
  public void register(ILifecycleComponent component) {
    if (state != State.STOPPED) {
      throw new RuntimeException("Cannot register component with running adaptor");
    }
    components.add(component);
    if (component instanceof IRunnable) {
      log.debug("runnable " + component.getId() + " registered with adaptor");
      runnables.add(component);
      ((IRunnable) component).setMessageProcessor(this);
    } else {
      log.debug("component " + component.getId() + " registered with adaptor");
    }
    if (component instanceof ITransactionInitiator) {
      ((ITransactionInitiator)component).setTransactionManager(transactionManager);
    }
  }

  public ILifecycleComponent unregister(ILifecycleComponent component) {
    ILifecycleComponent match = null;
    if (state != State.STOPPED) {
      throw new RuntimeException("Cannot unregister component from running adaptor");
    }
    if (components.remove(component)) {
      match = component;
      runnables.remove(component);
    }
    return match;
  }

  public Response process(Message msg) {
    return processor.process(msg);
  }

  public void start() {
    exitCode = 0;
    registerComponents();
    
    if (state != State.STOPPED) {
      throw new RuntimeException("adaptor is currently " + state.toString());
    }
    
    try {
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      state = State.STARTED;
      validate();
      startNonRunnables();
      startRunnables();
  
      if (runRunnablesInCallingThread) {
        runRunnable();
      } else {
        runRunnableThreads();
      }
  
      log.info("waiting for runnables to stop");
      waitForRunnablesToStop();
      log.info("all runnables are stopped");
      stopNonRunnables();
    } catch (Throwable ex) {
      log.error("failed to start adaptor", ex);
      exitCode = 1;
    } finally {
      if (state != State.STOPPING) {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      }
      state = State.STOPPED;
    }
    
    if (getExitCode() != 0) {
      log.fatal("adaptor exited with " + getExitCode());
    }
  }

  public void stop() {
    stopRunnables();
    waitForRunnablesToStop();
    stopNonRunnables();
  }

  public void stopNoWait() {
    stopRunnables();
  }

  public void validate(List exceptions) {

    if (runnables.isEmpty()) {
      exceptions.add(new Exception("no runnables"));
    }

    if (runRunnablesInCallingThread && runnables.size() > 1) {
      exceptions.add(new Exception("runRunnablesInCallingThread == true but multiple runnables"));
    }

    for (Iterator iter = components.iterator(); iter.hasNext();) {
      IMessageProcessor processor = (IMessageProcessor) iter.next();
      if (processor instanceof ILifecycleComponent) {
        try {
          ((ILifecycleComponent) processor).validate(exceptions);
        } catch (Exception e) {
          log.error("validation exception for " + processor.toString() + " : ", e);
          exceptions.add(e);
        }
      }
    }
  }

  public void run() {
    if (runConfiguration != null) {
      runConfiguration.run(this);
    } else {
      start();
    }
  }

  public int getExitCode() {
    int exitCode = this.exitCode;
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      exitCode += runnable.getExitCode();
    }
    return exitCode;
  }

  private void validate() {
    List exceptions = new ArrayList();
    validate(exceptions);
    if (exceptions.size() > 0) {
      for (Iterator iter = exceptions.iterator(); iter.hasNext();) {
        Exception exception = (Exception) iter.next();
        log.error("validation exception", exception);
      }
      throw new RuntimeException("adaptor validation failed");
    }
  }

  private void runRunnable() {
    if (runnables.size() != 1) {
      throw new RuntimeException("cannot run runnable directly as there are " + runnables.size() + " runnables");
    }
    IRunnable runnable = (IRunnable) runnables.get(0);
    runnable.run();
  }

  private void waitForRunnablesToStop() {
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      runnable.waitForState(State.STOPPED);
    }
  }

  private void runRunnableThreads() {
    runnableThreads = new Thread[runnables.size()];
    int i = 0;
    for (Iterator iter = runnables.iterator(); iter.hasNext(); i++) {
      IRunnable runnable = (IRunnable) iter.next();
      runnableThreads[i] = new Thread(runnable);
      if (runnable.getId() != null) {
        runnableThreads[i].setName(runnable.toString());
      }
    }
    for (int j = 0; j < runnableThreads.length; j++) {
      runnableThreads[j].start();
    }
  }

  private void startRunnables() {
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      runnable.addListener(this);
      startLifecycleComponent(runnable);
    }
  }

  private void stopRunnables() {
    for (Iterator iter = runnables.iterator(); iter.hasNext();) {
      IRunnable runnable = (IRunnable) iter.next();
      runnable.removeListener(this);
      stopLifecycleComponent(runnable);
    }
  }

  private void interruptRunnables() {
    for (int i = 0; i < runnableThreads.length; i++) {
      runnableThreads[i].interrupt();
    }
  }

  private void stopLifecycleComponent(ILifecycleComponent c) {
    synchronized (c) {
      if (!c.isState(State.STOPPED)) {
        c.stop();
      }
    }
  }
  
  private void startLifecycleComponent(ILifecycleComponent c) {
    synchronized (c) {
      if (c.isState(State.STOPPED)) {
        c.start();
      }
    }
  }
  
  private void startNonRunnables() {
    for (Iterator iter = components.iterator(); iter.hasNext();) {
      ILifecycleComponent component = (ILifecycleComponent) iter.next();
      if (!runnables.contains(component)) {
        startLifecycleComponent(component);
      }
    }
  }

  private void stopNonRunnables() {
    for (Iterator iter = components.iterator(); iter.hasNext();) {
      ILifecycleComponent component = (ILifecycleComponent) iter.next();
      if (!runnables.contains(component)) {
        stopLifecycleComponent(component);
      }
    }
  }

  public void setExceptionWriteConnector(final IWriteConnector exceptionProcessor) {
    this.exceptionProcessor = new WriteNode("exceptionProcessor", exceptionProcessor);
  }
  
  public void setExceptionProcessor(final IMessageProcessor exceptionProcessor) {
    this.exceptionProcessor = exceptionProcessor;
  }
  
  public void setPipeline(final Object[] pipeline) {
    if (exceptionProcessor != null) {
      setMessageProcessor(new Pipeline(pipeline, exceptionProcessor));
    } else {
      setMessageProcessor(new Pipeline(pipeline));
    }
  }
  
  public void exit() {
    state = State.STOPPING;
    if (runConfiguration != null) {
      runConfiguration.setExitFlag();
    }
    stop();
  }
  
  public class ShutdownHook extends Thread {
    public void run() {
      log.info("shutdownhook invoked, calling exit()");
      try {
        Adaptor.this.exit();
      } catch (Throwable t) {
        log.error("uncaught error or exception", t);
      }
    }
  }

  public void stateChanged(ILifecycleComponent component, State newState) {
    if (state == State.STARTED && runnables.contains(component) && newState == State.STOPPED) {
      if (((ReadNode)component).getExitCode() != 0) {
        log.warn(component.getId() + " has exited with non zero exit code, stopping adaptor");
        stopNoWait();
      }
    }
  }

  public Object getAdmin() {
    return new Admin();
  }
  
  public interface AdminMBean {
    String dumpState();
    void exit();
    void interrupt();
  }
  
  public class Admin implements AdminMBean {

    public void exit() {
      Adaptor.this.exit();
    }

    public String dumpState() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<table>");
      buffer.append("<tr><td>Adaptor</td><td/>");
      buffer.append("<td>").append(Adaptor.this.state.toString()).append("</td></tr>");
      ArrayList components = new ArrayList();
      components.addAll(Adaptor.this.components);
      for (Iterator iter = components.iterator(); iter.hasNext();) {
        ILifecycleComponent component = (ILifecycleComponent) iter.next();
        buffer.append("<tr><td/>");
        buffer.append("<td>").append(component.getId()).append("</td>");
        buffer.append("<td>").append(component.getState().toString()).append("</td></tr>");
      }
      return buffer.toString();
    }

    public void interrupt() {
      Adaptor.this.interruptRunnables();
    }
    
  }
}