package com.roposo.core.events;


import java.util.HashMap;

/**
 * @author amud on 18/09/17.
 */

public class RoposoEventMap extends HashMap<String, String> {

    public RoposoEventMap() {
        super(50);
    }

    @Override
    public String put(String key, String value) {
        /*if (size() == 50) {
            throw  new IllegalArgumentException("attribute size over limit of 50");
        }*/
        if (key == null) {
            throw new IllegalArgumentException("null attribute name");
        }

        if (!EventUtil.isValid(key)) {
            key = key.replaceAll(" ", "_").toLowerCase();
        }

        if (key.length() > 20) {
            key = key.substring(0, 20);
            Exception e = new IllegalArgumentException(key + " attribute name over");
            e.printStackTrace();
        }

        //todo to be retink
       /* if (value != null && value.length() > 50) {
            value = value.substring(0, 50);
            Exception e = new IllegalArgumentException(key + " attribute value over");
            e.printStackTrace();
        }*/
        return super.put(key, value);
    }



}
