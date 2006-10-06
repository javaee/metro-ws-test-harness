package jaxws.twoServices;

import javax.jws.WebService;

@WebService
public class Foo {
    public int echo(int x) { return x; }
}
