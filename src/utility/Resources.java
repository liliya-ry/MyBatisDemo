package utility;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

public class Resources {
    public static Reader getResourcesAsReader(String resource) throws FileNotFoundException {
        return new FileReader(resource);
    }
}
