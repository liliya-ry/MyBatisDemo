package utility;

import java.time.Instant;
import java.util.*;

public class FifoCache<K, V> implements Cache<K, V> {
    private final LinkedHashMap<K, V> itemsMap;
    private final long flushInterval;
    private final Timer timer = new Timer();
    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            itemsMap.clear();
        }
    };

    public FifoCache(int capacity, long flushInterval) {
        this.flushInterval = flushInterval;
        this.itemsMap = new LinkedHashMap<>(capacity) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > capacity;
            }
        };

        this.timer.scheduleAtFixedRate(timerTask, Date.from(Instant.now()), this.flushInterval);
    }

    @Override
    public void set(K key, V value) {
        itemsMap.put(key, value);
    }

    @Override
    public V get(K key) {
        return itemsMap.get(key);
    }

    @Override
    public void flushCache() {
        itemsMap.clear();
        this.timer.scheduleAtFixedRate(timerTask, Date.from(Instant.now()), this.flushInterval);
    }
}
