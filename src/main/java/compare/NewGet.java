/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package compare;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.epics.pva.client.ClientChannelListener;
import org.epics.pva.client.ClientChannelState;
import org.epics.pva.client.PVAChannel;
import org.epics.pva.client.PVAClient;

/** PVA 'get'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NewGet
{
    static
    {
        // TODO While running, stop the IOC and then restart
        //      ==> See what happens when disconnected while 1 channel was resolved,
        //          others still incomplete
        try
        {
            // LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/logging.properties"));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static AtomicInteger new_updates = new AtomicInteger();
    private static boolean print = false;

    private static void run_pvaclient()
    {
        try
        {
            PVAClient client = new PVAClient();
            while (true)
            {
                final CountDownLatch connected = new CountDownLatch(3);
                ClientChannelListener listener = (channel, state) ->
                {
                    if (state == ClientChannelState.CONNECTED)
                        connected.countDown();
                };
                PVAChannel ramp = client.getChannel("ramp", listener);
                PVAChannel saw = client.getChannel("saw", listener);
                PVAChannel rnd = client.getChannel("rnd", listener);
                connected.await();
                final String val1 = ramp.read("").get().get("value").toString();
                final String val2 = saw.read("").get().get("value").toString();
                final String val3 = rnd.read("").get().get("value").toString();
                if (print)
                {
                    System.out.println("ramp = " + val1);
                    System.out.println("saw = " + val2);
                    System.out.println("rnd = " + val3);
                }
                rnd.close();
                saw.close();
                ramp.close();
                new_updates.incrementAndGet();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    // Example result w/o printout: ~370 per second
    public static void main(String[] args) throws InterruptedException
    {
        final long start = System.currentTimeMillis();
        new Thread(NewGet::run_pvaclient).start();
        while (true)
        {
            Thread.sleep(5000);
            long updates = 1000*new_updates.get() / (System.currentTimeMillis() - start);
            System.out.println("New : " + updates + " per second");
        }
    }
}
