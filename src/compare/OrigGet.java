/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package compare;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.epics.pvaccess.ClientFactory;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistry;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;

/** PVA 'get' with original PVA library
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class OrigGet extends CompletableFuture<String> implements ChannelRequester, ChannelGetRequester
{
    private static ChannelProvider provider;

    static
    {
        //System.setProperty("EPICS_PVA_DEBUG", "3");
        ClientFactory.start();
        final ChannelProviderRegistry registry = ChannelProviderRegistryFactory.getChannelProviderRegistry();
        provider = registry.getProvider("pva");
    }

    final private static CreateRequest request_creater = CreateRequest.create();
    final private static PVStructure read_request = request_creater.createRequest("field()");

    final private Channel channel;
    private ChannelGet get;

    OrigGet(final String name) throws Exception
    {
        channel = provider.createChannel(name, this, ChannelProvider.PRIORITY_DEFAULT);
    }

    // ChannelRequester
    @Override
    public String getRequesterName()
    {
        return getClass().getName();
    }

    // ChannelRequester
    @Override
    public void message(final String message, final MessageType type)
    {
        // System.out.println("Message " + type + ": " + message);
    }

    // ChannelRequester
    @Override
    public void channelCreated(final Status status, final Channel channel)
    {
        if (!status.isSuccess())
            System.out.println("Channel " + channel.getChannelName() + " create problem: " + status.getMessage());
    }

    // ChannelRequester
    @Override
    public void channelStateChange(final Channel channel, final ConnectionState state)
    {
        switch (state)
        {
        case CONNECTED:
            get = channel.createChannelGet(this, read_request);
            break;
        case DISCONNECTED:
        default:
            // System.out.println("Channel " + channel.getChannelName() + " state: " + state);
        }
    }

    @Override
    public void channelGetConnect(Status status, ChannelGet get, Structure introspection)
    {
        if (status.isSuccess())
            get.get();
    }

    @Override
    public void getDone(Status status, ChannelGet get, PVStructure data,
            BitSet changed)
    {
        complete(data.getSubField("value").toString());
        // complete(data.toString());
    }

    public void close()
    {
        channel.destroy();
    }

    private static AtomicInteger orig_updates = new AtomicInteger();
    private static boolean print = false;

    private static void run_orig()
    {
        try
        {
            while (true)
            {
                final OrigGet ramp = new OrigGet("ramp");
                final OrigGet saw = new OrigGet("saw");
                final OrigGet rnd = new OrigGet("rnd");
                final String val1 = ramp.get();
                final String val2 = saw.get();
                final String val3 = rnd.get();
                if (print)
                {
                    System.out.println("ramp = " + val1);
                    System.out.println("saw = " + val2);
                    System.out.println("rnd = " + val3);
                }
                rnd.close();
                saw.close();
                ramp.close();
                orig_updates.incrementAndGet();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    // Example result w/o printout: ~90 per second
    public static void main(String[] args) throws InterruptedException
    {
        final long start = System.currentTimeMillis();
        new Thread(OrigGet::run_orig).start();
        while (true)
        {
            Thread.sleep(5000);
            long updates = 1000*orig_updates.get() / (System.currentTimeMillis() - start);
            System.out.println("Orig: " + updates + " per second");
        }
    }
}