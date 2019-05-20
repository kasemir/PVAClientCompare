/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.npva;

import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVANumber;
import org.epics.pva.data.PVAStructure;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;

@SuppressWarnings("nls")
public class PVAStructureHelper
{
    public static VType getVType(final PVAStructure struct) throws Exception
    {
        // Handle normative types
        String type = struct.getStructureName();
        if (type.startsWith("epics:nt/"))
            type = type.substring(9);
        if (type.equals("NTScalar:1.0"))
            return decodeNTScalar(struct);
//        if (type.equals("NTEnum:1.0"))
//            return Decoders.decodeEnum(data);
//        if (type.equals("NTScalarArray:1.0"))
//            return decodeNTArray(data);
//        if (type.equals("NTNDArray:1.0"))
//            return ImageDecoder.decode(data);
//        if (type.equals("NTTable:1.0"))
//            return decodeNTTable(data);

        // TODO Auto-generated method stub
        return null;
    }

    private static VType decodeNTScalar(final PVAStructure struct) throws Exception
    {
        final PVAData field = struct.get("value");
        if (field == null)
            throw new Exception("Expected struct with scalar 'value', got " + struct);
        if (field instanceof PVADouble)
            return Decoders.decodeDouble(struct, (PVADouble) field);
        // TODO Auto-generated method stub

        return VString.of(struct.format(),
                Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.CLIENT, "Unknown scalar type"),
                Time.now());
    }

    public static double getDoubleValue(final PVAStructure struct, String name,
                                        final double default_value)
    {
        final PVANumber field = struct.get(name);
        if (field != null)
            return field.getNumber().doubleValue();
        else
            return default_value;
    }
}
