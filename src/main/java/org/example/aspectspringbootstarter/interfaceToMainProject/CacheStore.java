package org.example.aspectspringbootstarter.interfaceToMainProject;

public interface CacheStore {
    public void put(String key,Object value,long ttl);
    public Object get(String key);
}
