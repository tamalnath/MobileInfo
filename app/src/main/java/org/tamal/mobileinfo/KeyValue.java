package org.tamal.mobileinfo;

class KeyValue implements Comparable<KeyValue> {

    String key;
    String kUrl;
    Object value;
    String vUrl;

    @Override
    public int compareTo(KeyValue o) {
        return key.compareTo(o.key);
    }

}
