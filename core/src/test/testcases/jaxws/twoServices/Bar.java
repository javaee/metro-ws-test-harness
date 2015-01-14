package jaxws.twoServices;

import javax.jws.WebService;

@WebService(portName="bar")
public class Bar {
    public int echo(int x) { return x; }
}
