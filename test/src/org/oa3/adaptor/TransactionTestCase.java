package org.oa3.adaptor;

import java.util.HashMap;
import java.util.Map;

import org.oa3.connector.TestReadConnector;
import org.oa3.connector.TestWriteConnector;
import org.oa3.processor.TestProcessor;
import org.oa3.router.RoutingMap;

import junit.framework.TestCase;

public class TransactionTestCase extends TestCase {

  /**
   * test that we get the correct number of commits on transactional resources
   *
   */
  public void testCommitAll() {

    TestReadConnector inpoint = new TestReadConnector("i");
    inpoint.setDataString("x");
    inpoint.setMaxSend(5);
    inpoint.setTransactional(true);
    inpoint.setExpectedCommitCount(5);
    
    TestProcessor processor = new TestProcessor("p");

    TestWriteConnector outpoint = new TestWriteConnector("o");
    outpoint.setExpectedOutput(AdaptorTestCase.createStringList("p(x)", 5));
    outpoint.setTransactional(true);
    outpoint.setExpectedCommitCount(5);
   
    // create routing map
    RoutingMap routingMap = new RoutingMap();
    
    Map processMap = new HashMap();
    processMap.put(inpoint, processor);
    processMap.put(processor, outpoint);
    routingMap.setProcessMap(processMap);
    
    // create adaptor
    Adaptor adaptor = new Adaptor();
    adaptor.setRoutingMap(routingMap);
    adaptor.setRunInpointsInCallingThread(true);

    // run adaptor
    adaptor.run();
    
    assertTrue(adaptor.getExitCode() == 0);

  }

  /**
   * test that we get the correct number of commits on transactional resources
   *
   */
  public void testCommitAllWithBatch() {

    TestReadConnector inpoint = new TestReadConnector("i");
    inpoint.setDataString("x");
    inpoint.setMaxSend(5);
    inpoint.setBatchSize(2);
    inpoint.setTransactional(true);
    inpoint.setExpectedCommitCount(5);
    
    TestProcessor processor = new TestProcessor("p");

    TestWriteConnector outpoint = new TestWriteConnector("o");
    outpoint.setExpectedOutput(AdaptorTestCase.createStringList("p(x)", 5));
    outpoint.setTransactional(true);
    outpoint.setExpectedCommitCount(5);
   
    // create routing map
    RoutingMap routingMap = new RoutingMap();
    
    Map processMap = new HashMap();
    processMap.put(inpoint, processor);
    processMap.put(processor, outpoint);
    routingMap.setProcessMap(processMap);
    
    // create adaptor
    Adaptor adaptor = new Adaptor();
    adaptor.setRoutingMap(routingMap);
    adaptor.setRunInpointsInCallingThread(true);

    // run adaptor
    adaptor.run();
    
    assertTrue(adaptor.getExitCode() == 0);

  }

  /**
   * test that write connector exceptions are trapped and routed correctly
   *
   */
  public void testCaughtOutpointException() {

    TestReadConnector inpoint = new TestReadConnector("i");
    inpoint.setDataString("x");
    inpoint.setMaxSend(5);
    inpoint.setTransactional(true);
    inpoint.setExpectedCommitCount(5);

    TestProcessor processor = new TestProcessor("p");

    TestWriteConnector outpoint = new TestWriteConnector("o");
    outpoint.setExpectedOutput(AdaptorTestCase.createStringList("p(x)", 3));
    outpoint.setExceptionFrequency(2);
    outpoint.setTransactional(true);
    outpoint.setExpectedCommitCount(3);

    TestWriteConnector errorOutpoint = new TestWriteConnector("e");
    errorOutpoint.setExpectedOutput(AdaptorTestCase.createStringList("java.lang.RuntimeException:test:p(x)", 2));

    // create routing map
    RoutingMap routingMap = new RoutingMap();
    
    Map processMap = new HashMap();
    processMap.put(inpoint, processor);
    processMap.put(processor, outpoint);
    routingMap.setProcessMap(processMap);
    
    Map exceptionMap = new HashMap();
    exceptionMap.put("java.lang.Exception", errorOutpoint);
    routingMap.setExceptionMap(exceptionMap);

    // create adaptor
    Adaptor adaptor = new Adaptor();
    adaptor.setRoutingMap(routingMap);
    adaptor.setRunInpointsInCallingThread(true);

    // run adaptor
    adaptor.run();
    
    assertTrue(adaptor.getExitCode() == 0);

  }

  /**
   * test that write connector exception causes adaptor to fail if there
   * is no exception mapping
   *
   */
  public void testUncaughtOutpointException() {

    TestReadConnector inpoint = new TestReadConnector("i");
    inpoint.setDataString("x");
    inpoint.setMaxSend(5);
    inpoint.setBatchSize(2);
    inpoint.setTransactional(true);
    inpoint.setExpectedCommitCount(5);

    TestProcessor processor = new TestProcessor("p");

    TestWriteConnector outpoint = new TestWriteConnector("o");
    outpoint.setExpectedOutput(AdaptorTestCase.createStringList("p(x)", 3));
    outpoint.setExceptionFrequency(4);
    inpoint.setTransactional(true);
    inpoint.setExpectedCommitCount(2);

    // create routing map
    RoutingMap routingMap = new RoutingMap();
    
    Map processMap = new HashMap();
    processMap.put(inpoint, processor);
    processMap.put(processor, outpoint);
    routingMap.setProcessMap(processMap);
    
    Map exceptionMap = new HashMap();
    routingMap.setExceptionMap(exceptionMap);

    // create adaptor
    Adaptor adaptor = new Adaptor();
    adaptor.setRoutingMap(routingMap);
    adaptor.setRunInpointsInCallingThread(true);

    // run adaptor
    adaptor.run();
    
    assertTrue(adaptor.getExitCode() != 0);
  }
  

}
