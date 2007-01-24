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
package org.openadaptor.auxil.connector.iostream.reader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openadaptor.core.exception.ComponentException;

/**
 * StreamReader which connects to, and reads from, the supplied URL.
 * 
 * @author Eddy Higgins
 */
public class URLReader extends AbstractStreamReader {

  private static final Log log = LogFactory.getLog(URLReader.class);

  /**
   * This is the url which will be used to connect to.
   */
  protected URL url;

  /**
   * Connection to the URL.
   */
  protected URLConnection urlConnection;

  // BEGIN Bean getters/setters

  /**
   * Set the url which this reader will connect to.
   * <p>
   * This is a convenience wrapper around setUrl(new URL(url));
   * 
   * @param url
   *          String which contains the url.
   * @throws MalformedURLException
   *           if the url <code>String</code> is invalid.
   */
  public void setUrl(String url) throws MalformedURLException {
    setUrl(new URL(url));
  }

  /**
   * Set the url to which this reader will connect to.
   * 
   * @param url
   *          <code>URL</code> to connect to.
   */
  public void setUrl(URL url) {
    this.url = url;
  }

  /**
   * Returns the <code>URL</code> that this reader will connect to.
   * 
   * @return the <code>URL</code> that this reader will connect to.
   */
  public URL getUrl() {
    return url;
  }

  // END Bean getters/setters

  /**
   * Establish a connection to a URL.
   * <p>
   * This will connect to a configured URL and obtain an <code>InputStream</code> from the connection. This will in
   * turn be used by the base class to get an appropriate <code>Reader</code>.
   * 
   * @throws org.openadaptor.control.ComponentException
   */
  public void connect() throws ComponentException {
    try {
      log.debug("Opening URL " + url);
      urlConnection = url.openConnection();
      _inputStream = urlConnection.getInputStream();

      /**
       * Flag the source of these records.
       */
      super.setReaderContext(url);
      super.connect();
    } catch (IOException ioe) { // Only catching exceptions that the super class doesn't
      log.error("Failed to open url - " + url + ". Exception - " + ioe.toString());
      throw new ComponentException("Failed to open url " + url, ioe, this);
    }
  }

  /**
   * Disconnect this <code>Reader</code>.
   * 
   * @throws org.openadaptor.control.ComponentException
   *           if there's a problem with the disconnect.
   */
  public void disconnect() {
    log.debug("Disconnecting from " + url);
    super.disconnect();
  }
}