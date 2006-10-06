package jaxws.twoServices;

import javax.jws.WebService;

@WebService
public class Bar {
    public int echo(int x) { return x; }
}
