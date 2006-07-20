/*
 * Main.java
 *
 * Created on July 19, 2006, 10:46 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package gfdeployer;

import javax.enterprise.deploy.spi.TargetModuleID;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 *
 * @author ken
 */
public class Main {

    public static void main(String[] args) throws Exception {
        JSR88Deployer deployer = new JSR88Deployer("deployer:Sun:AppServer::localhost:4848","admin","adminadmin");

        TargetModuleID[] modules = deployer.deploy(new File("test.war"), null, null);
        System.out.println("deployed");
        deployer.start(modules);
        System.out.println("started");

        new BufferedReader(new InputStreamReader(System.in)).readLine();

        deployer.undeploy(modules);
    }
}
