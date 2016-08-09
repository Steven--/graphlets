package wsdm16.motifs.randomwalks;

import java.util.LinkedHashMap;
import java.util.Map;

/** An LRU cache. Picked from http://chriswu.me/blog/a-lru-cache-in-10-lines-of-java/.
 * 
 * LRUCache.java - created on 29 lug 2016
 * @author anon
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
  private int cacheSize;

  public LRUCache(int cacheSize) {
    super(cacheSize, 0.75f, true);
    this.cacheSize = cacheSize;
  }

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() >= cacheSize;
  }
}

