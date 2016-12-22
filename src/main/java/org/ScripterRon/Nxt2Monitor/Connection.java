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

/**
 * Connection defines an NRS server connection
 */
public class Connection {

    /** Server host name */
    private String host;

    /** Server API port */
    private int port;

    /** Server administrator password */
    private String adminPW;

    /**
     * Create a server connection
     *
     * @param       host            Server host name
     * @param       port            Server API port or -1
     * @param       adminPW         Server administrator password or an empty string
     */
    public Connection(String host, int port, String adminPW) {
        this.host = host;
        this.port = port;
        this.adminPW = adminPW;
    }

    /**
     * Create a server connection
     *
     * The connection string has the format "host:port;adminPW".  The host will
     * be set to 'localhost' if it is not specified, the port will be set to
     * -1 if it is not specified and the adminPW will be set to an empty
     * string if it is not specified.
     *
     * @param       connect                     Server connection string
     * @throws      IllegalArgumentException    The connection string is not valid
     * @throws      NumberFormatException       Invalid port number
     */
    public Connection(String connect) throws IllegalArgumentException, NumberFormatException {
        host = "localhost";
        port = -1;
        adminPW = "";
        if (connect.length() != 0) {
            int sep, pos;
            if (connect.charAt(0) == '[') {
                sep = connect.indexOf(']') + 1;
                if (sep < 3)
                    throw new IllegalArgumentException(String.format("Invalid IPv6 address: %s", connect));
                host = connect.substring(0, sep);
            } else {
                sep = connect.indexOf(':');
                if (sep < 0)
                    sep = connect.indexOf(';');
                if (sep < 0)
                    host = connect;
                else if (sep > 0)
                    host = connect.substring(0, sep);
            }
            if (sep >= 0 && sep < connect.length()-1 && connect.charAt(sep) == ':') {
                pos = sep + 1;
                sep = connect.indexOf(';', pos);
                if (sep < 0)
                    port = Integer.valueOf(connect.substring(pos));
                else
                    port = Integer.valueOf(connect.substring(pos, sep));
            }
            if (sep >= 0 && sep < connect.length()-1)
                adminPW = connect.substring(sep+1);
        }
    }

    /**
     * Return the host name
     *
     * @return                      Host name
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host name
     *
     * @param       host            Host name
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Return the host port
     *
     * @return                      Host port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the host port
     *
     * @param       port            Host port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Return the administrator password
F    *
     * @return                      Administrator password
     */
    public String getAdminPW() {
        return adminPW;
    }

    /**
     * Set the administrator password
     *
     * @param       adminPW         Administrator password
     */
    public void setAdminPW(String adminPW) {
        this.adminPW = adminPW;
    }

    /**
     * Return the string representation of this connection
     *
     * @return                      String host:port;adminPW
     */
    @Override
    public String toString() {
        return String.format("%s:%d;%s", host, port, adminPW);
    }
}
