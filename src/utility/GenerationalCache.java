package utility;

import java.time.Instant;
import java.util.*;

public class GenerationalCache<K, V> implements Cache<K, V> {
    private static class CacheItem<K, V> {
        private static int itemCount = 0;
        K key;
        V value;
        int index;

        CacheItem(K key, V value) {
            this.key = key;
            this.value = value;
            this.index = itemCount++;
        }
    }

    private static final int GENERATION = 10;

    private PriorityQueue<CacheItem<K, V>> readsPriorityQueue;
    private final Map<K, CacheItem<K, V>> cacheItemsMap;
    private int[] currentReads;
    private int[] previousReads;
    private final int capacity;
    private final long flushInterval;
    private int readCount = 0;
    private final Timer timer = new Timer();
    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            cacheItemsMap.clear();
        }
    };

    public GenerationalCache(int capacity, long flushInterval) {
        this.capacity = capacity;
        this.flushInterval = flushInterval;
        this.cacheItemsMap = new HashMap<>(this.capacity);
        this.currentReads = new int[this.capacity];
        this.previousReads = new int[this.capacity];
        createPriorityQueue();
        this.timer.scheduleAtFixedRate(timerTask, Date.from(Instant.now()), this.flushInterval);
    }

    private void createPriorityQueue() {
        this.readsPriorityQueue = new PriorityQueue<>(
                (item1, item2) ->
                        Integer.compare(
                                getTotalReadsByIndex(item1.index),
                                getTotalReadsByIndex(item2.index)));
    }

    public void set(K key, V value) {
        CacheItem<K, V> item = cacheItemsMap.get(key);
        if (item != null) {
            item.value = value;
            return;
        }

        if (cacheItemsMap.size() == capacity) {
            evict();
        }

        item = new CacheItem<>(key, value);
        cacheItemsMap.put(key, item);
        readsPriorityQueue.add(item);
    }

    private void evict() {
        CacheItem<K, V> leastUsedItem = readsPriorityQueue.poll();
        cacheItemsMap.remove(leastUsedItem.key);
    }

    public V get(K key) {
        if (readCount == GENERATION) {
            changeGeneration();
        }

        readCount++;

        CacheItem<K, V> item = cacheItemsMap.get(key);
        if (item == null) {
            return null;
        }

        currentReads[item.index]++;
        readsPriorityQueue.remove(item);
        readsPriorityQueue.add(item);

        return item.value;
    }

    @Override
    public void flushCache() {
        cacheItemsMap.clear();
    }

    private void changeGeneration() {
        int[] temp = previousReads;
        previousReads = currentReads;
        currentReads = temp;

        Arrays.fill(currentReads, 0);
        readCount = 0;

        createPriorityQueue();
        cacheItemsMap.forEach((k, v) -> {
            readsPriorityQueue.add(v);
        });
    }

    private int getTotalReadsByIndex(int index) {
        return currentReads[index] + previousReads[index];
    }
}
