/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package compare;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.epics.pva.client.ClientChannelListener;
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
        try
        {
            // Verbose output
//            LogManager.getLogManager().readConfiguration(PVASettings.class.getResourceAsStream("/logging.properties"));
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
                final ClientChannelListener listener = (channel, state) -> {};
                final PVAChannel ramp = client.getChannel("ramp", listener);
                final PVAChannel saw = client.getChannel("saw", listener);
                final PVAChannel rnd = client.getChannel("rnd", listener);
                CompletableFuture.allOf(ramp.connect(), saw.connect(), rnd.connect()).get();
                                
                try
                {   // Once connected, allow 'get' to fail when IOC is shut down, ..
                    final String val1 = ramp.read("").get(2, TimeUnit.SECONDS).get("value").toString();
                    final String val2 = saw.read("").get(2, TimeUnit.SECONDS).get("value").toString();
                    final String val3 = rnd.read("").get(2, TimeUnit.SECONDS).get("value").toString();
                    if (print)
                    {
                        System.out.println("ramp = " + val1);
                        System.out.println("saw = " + val2);
                        System.out.println("rnd = " + val3);
                    }
                    new_updates.incrementAndGet();
                }
                catch (Exception ex)
                {   // log error, and try again
                    ex.printStackTrace();
                }
                rnd.close();
                saw.close();
                ramp.close();
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
