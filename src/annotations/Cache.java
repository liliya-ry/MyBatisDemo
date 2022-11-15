package annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {
    String memoryStoreEvictionPolicy();
    int maxEntriesLocalHeap();
    long timeToLiveSeconds();
}
