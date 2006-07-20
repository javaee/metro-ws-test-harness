

PingService service = new PingService();
IPing port = service.getWSHttpBindingIPing();
System.out.println("first message sent");
PingRequestBodyType p = new ObjectFactory().createPingRequestBodyType();
PingResponseBodyType pres = null;

for (i = 0; i<3; i++ ) {
    p.setText(new JAXBElement(new QName("http://tempuri.org/","Text"),String.class,"Hello There! no" + i));
    p.setSequence(new JAXBElement(new QName("http://tempuri.org/","Sequence"),String.class,"seq! no" + i));

    pres = port.echoString(p);
    System.out.println("Returned Value" + pres.getEchoStringReturn().getValue());
    String retStr = pres.getEchoStringReturn().getValue();
    assertNotNull(retStr);

}

ClientSession session = ClientSession.getSession((BindingProvider)port);
session.close();
