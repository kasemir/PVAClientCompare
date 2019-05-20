/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package compare;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.npva.PVA_PV;

/** Demo of async read for PV
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NewPVAsyncRead
{
    public static void main(String[] args) throws Exception
    {
        final PV pv = new PVA_PV("pva://ramp",  "ramp");
        // Await connection
        while (PV.isDisconnected(pv.read()))
            TimeUnit.MILLISECONDS.sleep(100);
        // Perform async read
        final Future<VType> value = pv.asyncRead();
        System.out.println(value.get(5, TimeUnit.SECONDS));
        // pv.close();
    }
}
