import org.red5.server.api.Red5;

/**
 * Provides information about the version of Red5 being used.
 * 
 * @author Paul Gregoire
 */
public class Version {

    public static void main(String[] args) {
        System.out.printf("Red5 version: %s %s%n", Red5.getVersion(), Red5.getFMSVersion());
    }

}
