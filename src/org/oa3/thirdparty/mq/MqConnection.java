/*
 * [[ Copyright (C) 2001 - 2006 The Software Conservancy as Trustee. All rights
 * reserved. Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions: The
 * above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS
 * IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. Nothing in
 * this notice shall be deemed to grant any rights to trademarks, copyrights,
 * patents, trade secrets or any other intellectual property of the licensor or
 * any contributor except as expressly stated herein. No patent license is
 * granted separate from the Software, for code that you delete from the
 * Software, or for combinations of the Software with other software or
 * hardware. ]]
 */
package org.oa3.thirdparty.mq;
/*
 * File: $Header$
 * Rev:  $Revision$
 */
import java.io.IOException;

import javax.transaction.xa.XAException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oa3.core.exception.OAException;

import com.ibm.mq.*;

/**
 * MqConnection is responsible for all interaction with MQ. It is responsible
 * for making connections and actual reading and writing of data. It acquires
 * XAResources for distributed transaction management and interacts with local
 * transaction managers for local transaction management. 
 * <br><br> 
 * Since IBMS's mq.jar is freely downloadable but not distributable direct access to the mq
 * a non-functional stub-api has been mocked up. This allows oa3 to build even in the absence 
 * of mq.jar. This is not ideal but avoids thenecessity for conditional compilition being built 
 * into the build script.
 */
public class MqConnection { 

  private static final Log log = LogFactory.getLog(MqConnection.class);

  /** The name of the MQ queue manager */
  protected String managerName = null;

  /** The host name of the MQ queue manager */
  protected String hostName = null;

  /** The listen port of the MQ queue manager */
  protected int port = -1;

  /** The MQ user name */
  protected String userName = null;

  /** The MQ user password */
  protected String password = null;

  /** The MQ channel name */
  protected String channelName = null;

  /** The MQ queue name */
  protected String queueName = null;

  /** CCSID used by client - defaults to 819 which (I think) is ascii */
  protected int ccsid = 0;

  /**
   * Set to true if you wish to use XA Transactions, for example manage the
   * transactioning via JTA.
   */
  protected boolean useXA = false;

  /** Set to true if local transactions required. */
  protected boolean useLocalTransaction = false;

  protected MQQueueManager queueManager ;

  protected MQQueue queue ;

  private MQGetMessageOptions getMessageOptions = null;

  private MQPutMessageOptions putMessageOptions = null;

  /**
   * Request the application data to be converted, to conform to the
   * characterSet and encoding attributes of the MQMessage, before the data is
   * copied into the message buffer. Because data conversion is also applied as
   * the data is retrieved from the message buffer, applications do not usually
   * set this option.
   */
  protected boolean convertOption = false;

  /**
   * MQ character set switch. If true, and message character set is 285 UK
   * EBCDIC, then switch to character set 37 US EBCDIC. This allows workaround
   * if mq libraries do not support UK EBCDIC since numbers, letters, space, +, -
   * and fullstop are all the same
   */
  protected boolean _UK_US_EBCDIC_Switch = true;

  /** Rudimentary retry stuff */
  protected long connectionRetryInterval = 1000;

  protected String mqContextUserAndPassword = null;

  protected int mqCharacterSet = 0;

  protected boolean useAllContext = false;

  private Object transactionalResource;

  // Bean Properties

  /**
   * Returns the name of the MQ queue manager
   * 
   * @return mq manager name
   */
  public String getManagerName() {
    return managerName;
  }

  /**
   * Set the name of the MQ queue manager
   */
  public void setManagerName(String managerName) {
    this.managerName = managerName;
  }

  /**
   * Returns the host name of the MQ queue manager
   * 
   * @return hostname
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * The host name of the MQ queue manager
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Returns the listen port of the MQ queue manager
   * 
   * @return port number
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets the listen port of the MQ queue manager
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Returns the MQ user name
   * 
   * @return username
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Set the MQ user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Returns the MQ user password
   * 
   * @return password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the MQ user password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns the MQ channel name.
   * 
   * @return mq channel name
   */
  public String getChannelName() {
    return channelName;
  }

  /**
   * Set the MQ channel name.
   */
  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  /**
   * Returns the MQ queue name
   * 
   * @return mq queue name
   */
  public String getQueueName() {
    return queueName;
  }

  /**
   * Set the MQ queue name
   */
  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  /**
   * Returns the CCSID used by client - defaults to 819 which (I think) is ascii
   * 
   * @return CCSID
   */
  public int getCcsid() {
    return ccsid;
  }

  /**
   * Set the CCSID used by client - defaults to 819 which (I think) is ascii
   */
  public void setCcsid(int ccsid) {
    this.ccsid = ccsid;
  }

  /**
   * Request the application data to be converted, to conform to the
   * characterSet and encoding attributes of the MQMessage, before the data is
   * copied into the message buffer. Because data conversion is also applied as
   * the data is retrieved from the message buffer, applications do not usually
   * set this option.
   * 
   * @return boolean
   */
  public boolean isConvertOption() {
    return convertOption;
  }

  /**
   * Request the application data to be converted, to conform to the
   * characterSet and encoding attributes of the MQMessage, before the data is
   * copied into the message buffer. Because data conversion is also applied as
   * the data is retrieved from the message buffer, applications do not usually
   * set this option.
   */
  public void setConvertOption(boolean convertOption) {
    this.convertOption = convertOption;
  }

  /**
   * Set to true if you wish to use XA Transactions, for example manage the
   * transactioning via JTA.
   * 
   * @return boolean
   */
  public boolean isUseXA() {
    return useXA;
  }

  /**
   * Set to true if you wish to use XA Transactions, for example manage the
   * transactioning via JTA.
   */
  public void setUseXA(boolean useXA) {
    this.useXA = useXA;
  }

  /**
   * Set to true to ensure that MQ is configure to start transactions on
   * read/write. This allows Local Transaction Management to work. Default value
   * is false.
   * 
   * @return boolean
   */
  public boolean isUseLocalTransaction() {
    return useLocalTransaction;
  }

  /**
   * Set to true to ensure that MQ is configure to start transactions on
   * read/write. This allows Local Transaction Management to work. Default value
   * is false.
   */
  public void setUseLocalTransaction(boolean useLocalTransaction) {
    this.useLocalTransaction = useLocalTransaction;
  }

  /**
   * Determine whether to use all context
   * 
   * @return boolean
   */
  public boolean isUseAllContext() {
    return useAllContext;
  }

  /**
   * Determine whether to use all context
   */
  public void setUseAllContext(boolean useAllContext) {
    this.useAllContext = useAllContext;
  }

  /**
   * Returns retry interval
   * 
   * @return retry interval
   */
  public long getConnectionRetryInterval() {
    return connectionRetryInterval;
  }

  /**
   * Set retry interval
   */
  public void setConnectionRetryInterval(long connectionRetryInterval) {
    this.connectionRetryInterval = connectionRetryInterval;
  }

  /**
   * @return mq connection credentials
   */
  public String getMqContextUserAndPassword() {
    return mqContextUserAndPassword;
  }

  /**
   * Set mq connection credentials
   */
  public void setMqContextUserAndPassword(String mqContextUserAndPassword) {
    this.mqContextUserAndPassword = mqContextUserAndPassword;
  }

  /**
   * Charcter set in use. E.g. UK EBCDIC
   * 
   * @return character set id
   */
  public int getMqCharacterSet() {
    return mqCharacterSet;
  }

  /**
   * Set character set in use. E.g. UK EBCDIC
   */
  public void setMqCharacterSet(int mqCharacterSet) {
    this.mqCharacterSet = mqCharacterSet;
  }

  // End Bean Properties

  /**
   * Set up connection to MQ
   * 
   * @param forRead
   */
  public void connectToMQ(boolean forRead) {
    log.debug("MqSource starting connection");
    // initialize MQ now
    MQEnvironment.channel = getChannelName();
    MQEnvironment.hostname = getHostName();
    MQEnvironment.port = getPort();
    if ( getUserName() != null ) MQEnvironment.userID = getUserName();
    if ( getPassword() != null ) MQEnvironment.password = getPassword();
    if ( getCcsid() != 0 ) MQEnvironment.CCSID = getCcsid();

    if (isUseXA()) {
      try {
        MQXAQueueManager xaQueueManager = new MQXAQueueManager(getManagerName());
      queueManager = xaQueueManager.getQueueManager();
      transactionalResource = xaQueueManager.getXAResource();
      } catch (MQException mqe) {
       throw new OAException("Failed to create MQXAQueueManager with " +
       mqe.toString());
      } catch (XAException xae) {
       throw new OAException("Failed to create MQXAResource with " +
       xae.toString());
      }
    } else if (isUseLocalTransaction()) {
      try {
        queueManager = new MQQueueManager(getManagerName());
        transactionalResource = new MqTransactionalResource(this);
      }
      catch (Exception mqe) {
        throw new RuntimeException("Failed to create MQQueueManager.", mqe);
      }
    } else {
          queueManager = new MQQueueManager(getManagerName());
      transactionalResource = null;
    }

    try {
       if (forRead) { 
         queue = (queueManager.accessQueue(getQueueName(),
                   MQC.MQOO_INPUT_AS_Q_DEF, 
                   null, // default q manager 
                   null, // no dynamic q name 
                   null)); // no alternate user id 
         } else {
           queue = (queueManager.accessQueue(getQueueName(), 
               MQC.MQOO_OUTPUT,
               null, // default q manager 
               null, // no dynamic q name 
               null)); // no alternate user id 
        }
       

      if (forRead) createGetMessageOptions();
      else createPutMessageOptions();

    }
    catch (Exception mqe) {
      throw new RuntimeException("Failed to access queue with " + mqe.toString(), mqe);
    }

    log.debug("MqSource connection successful");
  }

  /**
   * Close queue via proxy
   * 
   * @throws OaMqException
   */
  public void close() {
    try {
      queue.close();
    } catch (MQException e) {
     new RuntimeException("Exception closing MQ Queue. " + e);
    }
  }

  /**
   * Fetch next record
   * 
   * @return String
   * @throws OaMqException
   */
  public String nextRecord()  {
    String messageString = null;
    try {
      //
      // get a message from MQ
      //
      MQMessage message = new MQMessage();
      queue.get(message, getMessageOptions);
      //
      // print debug string
      //
      if (log.isDebugEnabled()) {
        log.debug(getMessageDescription(message));
      }
      //
      // Perform any specifed character set switch from 285 (UK EBCDIC) to 37
      // (US EBCDIC).
      //

      if (message.characterSet == 285 && _UK_US_EBCDIC_Switch) {
        log.debug("Received message in charset 285 (UK EBCDIC) - switching to 37 (US EBCDIC)");
        message.characterSet = 37;
      }
      //
      // get message string
      //
      messageString = message.readString(message.getMessageLength());
    }
    catch (MQException mqe) {
      //
      // Attempt a reconnection !!
      //

      if (mqe.reasonCode == MQException.MQRC_CONNECTION_BROKEN) {
        try {
          log.debug("Connection broken - waiting " + connectionRetryInterval + " ms before attempting reconnection");
          Thread.currentThread();
          Thread.sleep(connectionRetryInterval);
        }
        catch (InterruptedException interruptedexception) {
          interruptedexception.printStackTrace();
        }
        connectToMQ(false);
        return null;
      }
      //
      // if there's no message there then it's not really an
      // exception so we just return null
      //

      if (mqe.reasonCode == MQException.MQRC_NO_MSG_AVAILABLE) return null;

    }
    catch (Exception ioe) {
      //
      // thrown when read string fails
      //
      throw new OAException("Caught Exception examining message, " + ioe.toString());
    }
    return messageString;
  }

  /**
   * Queue message
   * 
   * @param record
   */
  public void deliverMessage(String record) {
    MQMessage mqMsg = new MQMessage();

    try {
      mqMsg.clearMessage();
      mqMsg.format = MQC.MQFMT_STRING;
      mqMsg.applicationIdData = mqContextUserAndPassword;
      if (mqCharacterSet != 0) {
        mqMsg.characterSet = mqCharacterSet;
      }
      mqMsg.writeString(record);
      log.debug("Putting MQ message");
      queue.put(mqMsg, putMessageOptions);
    }
    catch (IOException e) {
      throw new RuntimeException("IOException writing to MQ.", e);
    }
    catch (MQException e) {
      log.error("Unable to deliver message to MQ. ReasonCode [" + e.reasonCode + "]");
      throw new RuntimeException("MQException writing to MQ.", e);
    }
  }

  // Transaction Support

  public Object getResource() {
    return transactionalResource;
  }

  // Non-XA Transaction Support

  /**
   * Rollback mq transaction
   */
  public void rollback() {
    try {
      log.debug("Rolling back MQ Transaction");
      queueManager.backout();
      log.debug("Rolling back MQ Transaction successful");
    }
    catch (Exception mqe) {
      throw new RuntimeException("MQ backout() failed. ", mqe);
    }
  }

  /**
   * Commit mq transaction
   */
  public void commit() {
    try {
      queueManager.commit();
      log.debug("MQ Transaction commit successful");
    }
    catch (Exception mqe) {
      throw new RuntimeException("MQ Transaction commit failed. ", mqe);
    }
  }

  // Message Options

  /**
   * Generate messageoptions for reading, taking transaction settings into
   * account. Ideally this method is run just once but it is safe to rerun it.
   */
  protected void createGetMessageOptions() {
    // MQ will implicitly start a transaction if given appropriately set
    // MessageOptions.
    //
    // if we want local transactions then we need to ensure that a transaction
    // is started.
    // This used to be the default behaviour, but has since changed. Thanks to
    // Jeremy Davies for this fix.
    //
    
     getMessageOptions = new MQGetMessageOptions(); 
     if (isUseLocalTransaction()) { 
       if (isConvertOption()) {
           getMessageOptions.options = MQC.MQPMO_SYNCPOINT | MQC.MQGMO_CONVERT; 
           }
     else { 
       getMessageOptions.options = MQC.MQPMO_SYNCPOINT; 
       } 
      }
  }

  /**
   * Generate messageoptions for writing, taking transaction settings into
   * account. Ideally this method is run just once but it is safe to rerun it.
   */
  protected void createPutMessageOptions() {
    // MQ will implicitly start a transaction if given appropriately set
    // MessageOptions.
    //
    // if we want local transactions then we need to ensure that a transaction
    // is started.
    // This used to be the default behaviour, but has since changed. Thanks to
    // Jeremy Davies for this fix.
    //
    
     putMessageOptions = new MQPutMessageOptions(); 
     if (useLocalTransaction) {
       if (useAllContext) { 
         putMessageOptions.options = MQC.MQPMO_SYNCPOINT | MQC.MQOO_SET_ALL_CONTEXT; 
         } else { 
           putMessageOptions.options = MQC.MQPMO_SYNCPOINT; 
         }
       }
 
  }
  
  protected String getMessageDescription(MQMessage message) throws IOException {
    StringBuffer buffer = new StringBuffer();
    
     buffer.append("Received MQ message with TotalMessageLength ").append(message.getTotalMessageLength());
     buffer.append(" MessageLength ").append(message.getMessageLength());
     buffer.append(" DataLength ").append( message.getDataLength());
     buffer.append(" Character Set ").append( message.characterSet );
     buffer.append(" Format ").append( message.format );
     buffer.append(" Encoding ").append( message.encoding );
     
     return buffer.toString();
    
  }

}
