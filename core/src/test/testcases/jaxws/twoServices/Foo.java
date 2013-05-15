package jaxws.twoServices;

import javax.jws.WebService;

@WebService(portName="foo")
public class Foo {
    public int echo(int x) { return x; }
}
