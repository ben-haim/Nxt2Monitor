/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Monitor;

import org.ScripterRon.JSON.JSONObject;
import org.ScripterRon.JSON.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Send an API request to the Nxt server
 */
public class Request {

    /** UTF-8 character set */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** Default connect timeout (milliseconds) */
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    /** Default read timeout (milliseconds) */
    private static final int DEFAULT_READ_TIMEOUT = 30000;

    /** SSL initialized */
    private static volatile boolean sslInitialized = false;

    /**
     * Add a peer to the server peer list and connect to the peer
     *
     * @param       announcedAddress        The announced address of the peer
     * @return                              Peer
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response addPeer(String announcedAddress) throws IOException {
        return issueRequest("addPeer",
                            String.format("peer=%s&adminPassword=%s",
                                URLEncoder.encode(announcedAddress, "UTF-8"),
                                URLEncoder.encode(Main.adminPW, "UTF-8")),
                            DEFAULT_READ_TIMEOUT);
    }

    /**
     * Blacklist a peer
     *
     * @param       address                 Peer address
     * @return                              TRUE if the peer was blacklisted
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static boolean blacklistPeer(String address) throws IOException {
        Response response = issueRequest("blacklistPeer",
                                         String.format("peer=%s&adminPassword=%s",
                                             URLEncoder.encode(address, "UTF-8"),
                                             URLEncoder.encode(Main.adminPW, "UTF-8")),
                                         DEFAULT_READ_TIMEOUT);
        return response.getBoolean("done");
    }

    /**
     * Register wait events
     *
     * An existing event list can be modified by specifying 'addEvents=true' or 'removeEvents=true'.
     * A new event list will be created if both parameters are false.  An existing event listener
     * will be canceled if all of the registered events are removed.
     *
     * @param       events                  List of events to register
     * @param       addEvents               TRUE to add events to an existing event list
     * @param       removeEvents            TRUE to remove events from an existing event list
     * @return                              TRUE if the events were registered
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static boolean eventRegister(List<String> events, boolean addEvents, boolean removeEvents)
                                            throws IOException {
        StringBuilder sb = new StringBuilder(1000);
        for (String event : events) {
            if (sb.length() > 0)
                sb.append("&");
            sb.append("event=").append(URLEncoder.encode(event, "UTF-8"));
        }
        if (addEvents) {
            if (sb.length() > 0)
                sb.append("&");
            sb.append("add=true");
        }
        if (removeEvents) {
            if (sb.length() > 0)
                sb.append("&");
            sb.append("remove=true");
        }
        Response response = issueRequest("eventRegister",
                                         (sb.length()>0 ? sb.toString() : null),
                                         DEFAULT_READ_TIMEOUT);
        return response.getBoolean("registered");
    }

    /**
     * Wait for an event
     *
     * @param       timeout                 Wait timeout (seconds)
     * @return                              Event list
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static List<Event> eventWait(int timeout) throws IOException {
        List<Event> events = new ArrayList<>();
        Response response = issueRequest("eventWait",
                                         String.format("timeout=%d", timeout),
                                         (timeout+5)*1000);
        List<Map<String, Object>> eventList = response.getObjectList("events");
        eventList.forEach(resp -> events.add(new Event(new Response(resp))));
        return events;
    }

    /**
     * Get a block
     *
     * @param       blockId                 Block identifier
     * @param       includeTransactions     TRUE to include the block transactions or
     *                                      FALSE to include just the transaction identifiers
     * @return                              Block response
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getBlock(String blockId, boolean includeTransactions) throws IOException {
        return issueRequest("getBlock",
                            String.format("block=%s&includeTransactions=%s",
                                blockId, includeTransactions),
                            DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get a list of blocks
     *
     * @param       firstIndex              Start index (chain head is index 0)
     * @param       lastIndex               Stop index
     * @param       includeTransactions     TRUE to include the block transactions or
     *                                      FALSE to include just the transaction identifiers
     * @return                              Block list
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static List<Response> getBlocks(int firstIndex, int lastIndex, boolean includeTransactions)
                                            throws IOException {
        if (firstIndex < 0 || lastIndex < firstIndex)
            throw new IllegalArgumentException("Illegal index values");
        List<Response> blocks = new ArrayList<>(lastIndex - firstIndex + 1);
        Response response = issueRequest("getBlocks",
                                         String.format("firstIndex=%d&lastIndex=%d&includeTransactions=%s",
                                             firstIndex, lastIndex, includeTransactions),
                                         DEFAULT_READ_TIMEOUT);
        response.getObjectList("blocks").forEach(entry -> blocks.add(new Response(entry)));
        return blocks;
    }

    /**
     * Get the server bundler status
     *
     * @return                              List of bundlers
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getBundlers() throws IOException {
        return issueRequest("getBundlers",
                            String.format("adminPassword=%s",
                                    URLEncoder.encode(Main.adminPW, "UTF-8")),
                            DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the server constants
     *
     * @return                              Server constants
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getConstants() throws IOException {
        return issueRequest("getConstants", null, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the server forging status
     *
     * @return                              List of generators
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getForging() throws IOException {
        return issueRequest("getForging",
                            String.format("adminPassword=%s",
                                    URLEncoder.encode(Main.adminPW, "UTF-8")),
                            DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the server log
     *
     * @param       count                   Number of records to get
     * @return                              Log records
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getLog(int count) throws IOException {
        return issueRequest("getLog",
                            String.format("count=%d&adminPassword=%s",
                                    count, URLEncoder.encode(Main.adminPW, "UTF-8")),
                            DEFAULT_READ_TIMEOUT);
    }
    
    /**
     * Get a peer
     *
     * @param       networkAddress          The network address of the peer
     * @return                              Peer
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getPeer(String networkAddress) throws IOException {
        return issueRequest("getPeer",
                            "peer=" + URLEncoder.encode(networkAddress, "UTF-8"),
                            DEFAULT_READ_TIMEOUT);
    }

    /**
     * Get the current peer information
     *
     * @param       state                   Return peers in this state
     * @return                              Peer list
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static List<Response> getPeers(String state) throws IOException {
        List<Response> peers = new ArrayList<>();
        Response response = issueRequest("getPeers",
                                         String.format("state=%s&includePeerInfo=true", state),
                                         DEFAULT_READ_TIMEOUT);
        response.getObjectList("peers").forEach(entry -> peers.add(new Response(entry)));
        return peers;
    }

    /**
     * Get a transaction
     *
     * @param       txFullHash              Transaction full hash
     * @param       chain                   Transaction chain
     * @return                              Transaction
     * @throws      IOException             Unable to issue Nxt API request
     */
    public static Response getTransaction(String txFullHash, String chain) throws IOException {
        return issueRequest("getTransaction",
                            String.format("fullHash=%s&chain=%s", txFullHash, chain),
                            DEFAULT_READ_TIMEOUT);
    }

    /**
     * Set server logging
     *
     * @param       logLevel                Log level
     * @return                              TRUE if the logging level was updated
     * @throws      IOException             Unable to set server logging
     */
    public static boolean setLogging(String logLevel) throws IOException {
        Response response = issueRequest("setLogging",
                                         String.format("logLevel=%s&adminPassword=%s",
                                             logLevel, URLEncoder.encode(Main.adminPW, "UTF-8")),
                                         DEFAULT_READ_TIMEOUT);
        return response.getBoolean("loggingUpdated");
    }

    /**
     * Issue the Nxt API request and return the parsed JSON response
     *
     * @param       requestType             Request type
     * @param       requestParams           Request parameters
     * @param       readTimeout             Read timeout (milliseconds)
     * @return                              Parsed JSON response
     * @throws      IOException             Unable to issue Nxt API request
     */
    @SuppressWarnings("unchecked")
    private static Response issueRequest(String requestType, String requestParams, int readTimeout)
                                            throws IOException {
        Response response = null;
        if (Main.useSSL && !sslInitialized)
            sslInit();
        try {
            String host = Main.serverConnection.getHost();
            int port = Main.serverConnection.getPort();
            URL url = new URL(String.format("%s://%s:%d/nxt",
                    (host.equals("localhost") ? "http" : (Main.useSSL ? "https" : "http")), host, port));
            String request;
            if (requestParams != null)
                request = String.format("requestType=%s&%s", requestType, requestParams);
            else
                request = String.format("requestType=%s", requestType);
            byte[] requestBytes = request.getBytes(UTF8);
            Main.log.debug(String.format("Issue HTTP request to %s:%d: %s",
                    Main.serverConnection.getHost(), Main.serverConnection.getPort(), request));
            //
            // Issue the request
            //
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Cache-Control", "no-cache, no-store");
            conn.setRequestProperty("Content-Length", Integer.toString(requestBytes.length));
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            conn.setReadTimeout(readTimeout);
            conn.connect();
            try (OutputStream out = conn.getOutputStream()) {
                out.write(requestBytes);
                out.flush();
                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    String errorText = String.format("Response code %d for %s request\n  %s",
                                                     code, requestType, conn.getResponseMessage());
                    Main.log.error(errorText);
                    throw new IOException(errorText);
                }
            }
            //
            // Parse the response
            //
            String contentEncoding = conn.getHeaderField("Content-Encoding");
            try (InputStream in = conn.getInputStream()) {
                InputStreamReader reader;
                if ("gzip".equals(contentEncoding))
                    reader = new InputStreamReader(new GZIPInputStream(in), UTF8);
                else
                    reader = new InputStreamReader(in, UTF8);
                Object respObject = JSONParser.parse(reader);
                if (!(respObject instanceof JSONObject))
                    throw new IOException("Server response is not a JSON object");
                response = new Response((Map<String, Object>)respObject);
                Long errorCode = (Long)response.get("errorCode");
                if (errorCode != null) {
                    String errorDesc = (String)response.get("errorDescription");
                    String errorText = String.format("Error %d returned for %s request: %s",
                                                     errorCode, requestType, errorDesc);
                    Main.log.error(errorText);
                    throw new IOException(errorText);
                }
            }
            if (Main.log.isDebugEnabled())
                Main.log.debug(String.format("Request complete: Content-Encoding %s\n%s",
                                        contentEncoding, Utils.formatJSON(response.getObjectMap())));
        } catch (ParseException exc) {
            String errorText = String.format("JSON parse exception for %s request: Position %d: %s",
                                             requestType, exc.getErrorOffset(), exc.getMessage());
            Main.log.error(errorText);
            throw new IOException(errorText);
        } catch (IOException exc) {
            String errorText = String.format("I/O error on %s request", requestType);
            Main.log.error(errorText, exc);
            throw new IOException(errorText, exc);
        }
        return response;
    }

    /**
     * SSL initialization
     */
    private static void sslInit() {
        try {
            //
            // Create the SSL context
            //
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager[] tm = (Main.acceptAnyCertificate ? new TrustManager[] {new AllCertificates()} : null);
            context.init(null, tm, new SecureRandom());
            //
            // Set default values for HTTPS connections
            //
            HttpsURLConnection.setDefaultHostnameVerifier(new NameVerifier());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            sslInitialized = true;
        } catch (NoSuchAlgorithmException exc) {
            Main.log.error("TLS algorithm is not available", exc);
            throw new IllegalStateException("TLS algorithm is not available");
        } catch (KeyManagementException exc) {
            Main.log.error("Unable to initialize SSL context", exc);
            throw new IllegalStateException("Unable to initialize SSL context", exc);
        }
    }

    /**
     * Certificate host name verifier
     */
    private static class NameVerifier implements HostnameVerifier {

        /**
         * Check if a certificate host name mismatch is allowed
         *
         * @param       hostname            URL host name
         * @param       session             SSL session
         * @return                          TRUE if the mismatch is allowed
         */
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return Main.allowNameMismatch;
        }
    }

    /**
     * Certificate trust manager to accept all certificates
     */
    private static class AllCertificates implements X509TrustManager {

        /**
         * Return a list of accepted certificate issuers
         *
         * Since we accept all certificates, we will return an empty certificate list.
         *
         * @return                          Empty certificate list
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        /**
         * Build the certificate path to a trusted root certificate
         *
         * Since we accept all certificates, we will simply return
         *
         * @param   certs                   Certificate chain
         * @param   authType                Authentication type
         */
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType)
                                            throws CertificateException {
        }

        /**
         * Build the certificate path to a trusted root certificate
         *
         * Since we accept all certificates, we will simply return
         *
         * @param   certs                   Certificate chain
         * @param   authType                Authentication type
         */
        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType)
                                            throws CertificateException {
        }
    }
}
