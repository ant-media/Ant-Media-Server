import org.red5.server.api.Red5;

/**
 * Provides information about the version of Red5 being used.
 * 
 * @author Paul Gregoire
 */
public class Version {

    public static void main(String[] args) {
        System.out.printf("Ant Media Server Version: %s %s%n", io.antmedia.Version.class.getPackage().getImplementationVersion());
    }

}
