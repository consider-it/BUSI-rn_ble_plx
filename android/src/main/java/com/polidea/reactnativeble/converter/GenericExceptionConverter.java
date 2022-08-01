package com.polidea.reactnativeble.converter;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

public class GenericExceptionConverter {

    public ReadableArray toJSCallback(Exception error) {
        WritableArray array = Arguments.createArray();
        array.pushString(toJs(error));
        array.pushNull();
        return array;
    }

    public String toJs(Exception error) {
        WritableArray array = Arguments.createArray();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");

        stringBuilder.append("\"errorCode\":");
        stringBuilder.append(0);

        stringBuilder.append(",\"attErrorCode\":");
        stringBuilder.append("null");

        stringBuilder.append(",\"iosErrorCode\": null");

        stringBuilder.append(",\"androidErrorCode\":");
        stringBuilder.append("null");

        appendString(stringBuilder, "reason", error.getMessage());
        appendString(stringBuilder, "deviceID", "null");
        appendString(stringBuilder, "serviceUUID", "null");
        appendString(stringBuilder, "characteristicUUID", "null");
        appendString(stringBuilder, "descriptorUUID", "null");
        appendString(stringBuilder, "internalMessage", "null");

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

    private void appendString(StringBuilder stringBuilder, String key, String value) {
        stringBuilder.append(",\"");
        stringBuilder.append(key);
        stringBuilder.append("\":");
        if (value == null) {
            stringBuilder.append("null");
        } else {
            stringBuilder.append("\"");
            stringBuilder.append(value);
            stringBuilder.append("\"");
        }
    }
}