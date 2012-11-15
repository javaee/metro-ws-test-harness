/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

import java.awt.image.BufferedImage;
import javax.imageio.ImageWriter;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

byte[] getImageBytes(Image image, String type) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    BufferedImage bufImage = convertToBufferedImage(image);
    ImageWriter writer = null;
    Iterator i = ImageIO.getImageWritersByMIMEType(type);
    if (i.hasNext()) {
        writer = (ImageWriter)i.next();
    }
    if (writer != null) {
        ImageOutputStream stream = null;
        stream = ImageIO.createImageOutputStream(baos);
        writer.setOutput(stream);
        writer.write(bufImage);
        stream.close();
        return baos.toByteArray();
    }
    return null;
}

BufferedImage convertToBufferedImage (Image image) throws IOException {
    if (image instanceof BufferedImage) {
        return (BufferedImage)image;

    } else {
        MediaTracker tracker = new MediaTracker (null/*not sure how this is used*/);
        tracker.addImage (image, 0);
        try {
            tracker.waitForAll ();
        } catch (InterruptedException e) {
            throw new IOException (e.getMessage ());
        }
        BufferedImage bufImage = new BufferedImage (
            image.getWidth (null),
            image.getHeight (null),
            BufferedImage.TYPE_INT_RGB);

        Graphics g = bufImage.createGraphics ();
        g.drawImage (image, 0, 0, null);
        return bufImage;
    }
}

/**
 * Demonstrates xmime:expectedContentTypes annotation
 */

void testMtom (Hello port) throws Exception {
    name="Duke";
    photo = new Holder(name.getBytes());
    image = new Holder(javax.imageio.ImageIO.read (resource("java.jpg")));
    helloPort.detail (photo, image);
    if(new String (photo.value).equals (name) && (image.value != null))
        System.out.println ("SOAP 1.1 testMtom() PASSED!");
    else
        System.out.println ("SOAP 1.1 testMtom() FAILED!");
}

/**
 * Demonstrates a basic xs:base64Binary optimization
 */
void testEcho(Hello port) throws Exception{
    jpegFile = javax.imageio.ImageIO.read (resource("java.jpg"));
    byte[] bytes = getImageBytes(jpegFile, "image/jpeg");
    image = new Holder(bytes);
    port.echoData(image);
    if(image.value != null)
        System.out.println ("SOAP 1.1 testEcho() PASSED!");
    else
        System.out.println ("SOAP 1.1 testEcho() FAILED!");
}

//get the binding and enable mtom
binding = helloPort.getBinding ();
binding.setMTOMEnabled (true);

//test mtom
testMtom (helloPort);

//test echo
testEcho(helloPort);
