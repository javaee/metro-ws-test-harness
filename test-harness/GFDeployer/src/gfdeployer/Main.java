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
    
    public static void main(String[] args) throws Exception {
        String[] deployArgs = {"deploy", "deployer:Sun:AppServer::localhost:4848", "admin", "adminadmin", "true", "./test.war"};
        String[] undeployArgs = {"undeploy", "deployer:Sun:AppServer::localhost:4848", "admin", "adminadmin", "test"};

        JSR88Deployer.main(deployArgs);
        System.out.println("deployed");
        Thread.sleep(30000);
        JSR88Deployer.main(undeployArgs);
    }
}
