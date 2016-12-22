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

import org.ScripterRon.JSON.JSONAware;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility routines
 */
public class Utils {

    /**
     * Convert from NQT to a decimal string (1 NQT = 0.00000001 NXT)
     * We will keep at least 4 decimal places in the result.
     *
     * @param       value           Value to be converted
     * @return                      A formatted decimal string
     */
    public static String nqtToString(long value) {
        //
        // Format the amount
        //
        long bvalue = value;
        boolean negative = (bvalue < 0);
        if (negative)
            bvalue = -bvalue;
        //
        // Get the amount as a formatted string with 8 decimal places
        //
        String valueString = String.format("%09d", bvalue);
        int decimalPoint = valueString.length() - 8;
        //
        // Drop tailing zeros beyond 4 decimal places
        //
        int toDelete = 0;
        for (int i=valueString.length()-1; i>decimalPoint+3; i--) {
            if (valueString.charAt(i) != '0')
                break;
            toDelete++;
        }
        //
        // Create the formatted decimal string
        //
        StringBuilder formatted = new StringBuilder(valueString.substring(0, valueString.length()-toDelete));
        formatted.insert(decimalPoint, '.');
        //
        // Insert commas as needed
        //
        int index = decimalPoint;
        while (index > 3) {
            index -= 3;
            formatted.insert(index, ',');
        }
        //
        // Add the sign if the value is negative
        //
        if (negative)
            formatted.insert(0, '-');
        return formatted.toString();
    }

    /**
     * Convert a string to an object identifier
     *
     * @param       number                  Object identifier string
     * @return                              Object identifier
     * @throws      NumberFormatException   Invalid object identifier
     */
    public static long stringToId(String number) throws NumberFormatException {
        return number.length() != 0 ? Long.parseUnsignedLong(number) : 0;
    }

    /**
     * Convert an object identifier to a string
     *
     * @param       id                      Object identifier
     * @return                              Identifier string
     */
    public static String idToString(long id) {
        return Long.toUnsignedString(id);
    }

    /**
     * Parse a hex string and return the decoded bytes
     *
     * @param       hex                     String to parse
     * @return                              Decoded bytes
     * @throws      NumberFormatException   String contains an invalid hex character
     */
    public static byte[] parseHexString(String hex) throws NumberFormatException {
        try {
            if ((hex.length()&0x01) == 1)
                throw new NumberFormatException("Hex string length is not a multiple of 2");
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int char1 = hex.charAt(i * 2);
                char1 = char1 > 0x60 ? char1 - 0x57 : char1 - 0x30;
                int char2 = hex.charAt(i * 2 + 1);
                char2 = char2 > 0x60 ? char2 - 0x57 : char2 - 0x30;
                if (char1 < 0 || char2 < 0 || char1 > 15 || char2 > 15)
                    throw new NumberFormatException("Invalid hex number: " + hex);
                bytes[i] = (byte)((char1 << 4) + char2);
            }
            return bytes;
        } catch (Exception exc) {
            Main.log.debug("Invalid hex string: '" + hex + "'");
            throw exc;
        }
    }

    /**
     * Create a formatted string for a JSON array
     *
     * @param       array                   JSON array
     * @return                              Formatted string
     */
    public static String formatJSON(List<Object> array) {
        StringBuilder builder = new StringBuilder(512);
        formatJSON(builder, "", array);
        return new String(builder);
    }

    /**
     * Create a formatted string for a JSON object
     *
     * @param       map                     JSON object
     * @return                              Formatted string
     */
    public static String formatJSON(Map<String, Object> map) {
        StringBuilder builder = new StringBuilder(512);
        formatJSON(builder, "", map);
        return new String(builder);
    }

    /**
     * Create a formatted string for a JSON structure
     *
     * @param       builder                 String builder
     * @param       indent                  Output indentation
     * @param       object                  The JSON object
     */
    @SuppressWarnings("unchecked")
    private static void formatJSON(StringBuilder builder, String indent, Object object) {
        String itemIndent = indent+"  ";
        if (object instanceof List) {
            List<Object> array = (List<Object>)object;
            builder.append(indent).append("[\n");
            array.forEach((value) -> {
                if (value == null) {
                    builder.append(itemIndent).append("null").append('\n');
                } else if (value instanceof Boolean) {
                    builder.append(itemIndent).append((Boolean)value ? "true\n" : "false\n");
                } else if (value instanceof Long) {
                    builder.append(itemIndent).append(((Long)value).toString()).append('\n');
                } else if (value instanceof Double) {
                    builder.append(itemIndent).append(((Double)value).toString()).append('\n');
                } else if (value instanceof String) {
                    builder.append(itemIndent).append('"').append((String)value).append("\"\n");
                } else if ((value instanceof List) || (value instanceof Map)) {
                    formatJSON(builder, itemIndent, (JSONAware)value);
                } else {
                    builder.append(itemIndent).append("Unknown\n");
                }
            });
            builder.append(indent).append("]\n");
        } else {
            builder.append(indent).append("{\n");
            Map<String, Object> map = (Map<String, Object>)object;
            Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                builder.append(itemIndent).append("\"").append(entry.getKey()).append("\": ");
                Object value = entry.getValue();
                if (value == null) {
                    builder.append("null").append('\n');
                } else if (value instanceof Boolean) {
                    builder.append((Boolean)value ? "true\n" : "false\n");
                } else if (value instanceof Long) {
                    builder.append(((Long)value).toString()).append('\n');
                } else if (value instanceof Double) {
                    builder.append(((Double)value).toString()).append('\n');
                } else if (value instanceof String) {
                    builder.append('"').append((String)value).append("\"\n");
                } else if (value instanceof JSONAware) {
                    builder.append('\n');
                    formatJSON(builder, itemIndent+"  ", (JSONAware)value);
                } else {
                    builder.append("Unknown\n");
                }
            }
            builder.append(indent).append("}\n");
        }
    }
}
