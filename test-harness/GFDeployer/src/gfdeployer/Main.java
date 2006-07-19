/*
 * Main.java
 *
 * Created on July 19, 2006, 10:46 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gfdeployer;

/**
 *
 * @author ken
 */
public class Main {
    
    /** Creates a new instance of Main */
    public Main() {
    }

    public static void main(String[] args) {
        String[] deployArgs = {"deploy", "deployer:Sun:AppServer::localhost:4848", "admin", "adminadmin", "true", "./test.war"};
        String[] undeployArgs = {"undeploy", "deployer:Sun:AppServer::localhost:4848", "admin", "adminadmin", "test"};
        System.setProperty("com.sun.aas.installRoot","/home/ken/work/glassfish-b08");

        JSR88Deployer.main(deployArgs);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        JSR88Deployer.main(undeployArgs);
    }
}
